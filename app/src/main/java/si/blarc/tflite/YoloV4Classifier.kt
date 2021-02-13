/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package si.blarc.tflite

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.util.Log
import si.blarc.env.Logger
import si.blarc.env.Config.MINIMUM_CONFIDENCE_TF_OD_API
import si.blarc.env.Utils
import si.blarc.env.Utils.expit
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.exp

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 *
 *
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
open class YoloV4Classifier private constructor() : Classifier {

    override fun enableStatLogging(debug: Boolean) {}
    override val statString: String get() = ""

    override fun close() {}
    override fun setNumThreads(numThreads: Int) {
        if (tfLite != null) tfLite!!.setNumThreads(numThreads)
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        if (tfLite != null) tfLite!!.setUseNNAPI(isChecked)
    }

    override val objThresh: Float
        get() = MINIMUM_CONFIDENCE_TF_OD_API
    private var isModelQuantized = false

    // Config values.
    // Pre-allocated buffers.
    private val labels = Vector<String>()
    private lateinit var intValues: IntArray
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null

    //non maximum suppression
    private fun nms(list: ArrayList<Classifier.Recognition>): ArrayList<Classifier.Recognition?> {
        val nmsList: ArrayList<Classifier.Recognition?> = ArrayList()

        for (k in labels.indices) {
            //1. Find max confidence per class
            val priorityQueue: PriorityQueue<Classifier.Recognition> = PriorityQueue(50) { lhs, rhs ->

                // Intentionally reversed to put high confidence at the head of the queue.
                (rhs.confidence!!).compareTo(lhs.confidence!!)
            }

            for (i in list.indices) {
                if (list[i].detectedClass == k) {
                    priorityQueue.add(list[i])
                }
            }

            //2. Do non maximum suppression
            while (priorityQueue.size > 0) {
                // Insert detection with max confidence
                val a: Array<Classifier.Recognition?> = arrayOfNulls(priorityQueue.size)
                val detections: Array<Classifier.Recognition> = priorityQueue.toArray(a)
                val max: Classifier.Recognition = detections[0]
                nmsList.add(max)
                priorityQueue.clear()
                for (j in 1 until detections.size) {
                    val detection: Classifier.Recognition = detections[j]
                    val b: RectF = detection.getLocation()
                    if (boxIou(max.getLocation(), b) < mNmsThresh) {
                        priorityQueue.add(detection)
                    }
                }
            }
        }
        return nmsList
    }

    private var mNmsThresh = 0.6f
    private fun boxIou(a: RectF, b: RectF): Float {
        return boxIntersection(a, b) / boxUnion(a, b)
    }

    private fun boxIntersection(a: RectF, b: RectF): Float {
        val w = overlap(
            (a.left + a.right) / 2, a.right - a.left,
            (b.left + b.right) / 2, b.right - b.left
        )
        val h = overlap(
            (a.top + a.bottom) / 2, a.bottom - a.top,
            (b.top + b.bottom) / 2, b.bottom - b.top
        )
        return if (w < 0 || h < 0) 0F else w * h
    }

    private fun boxUnion(a: RectF, b: RectF): Float {
        val i = boxIntersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    private fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
        val l1 = x1 - w1 / 2
        val l2 = x2 - w2 / 2
        val left = if (l1 > l2) l1 else l2
        val r1 = x1 + w1 / 2
        val r2 = x2 + w2 / 2
        val right = if (r1 < r2) r1 else r2
        return right - left
    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer =
            ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                byteBuffer.putFloat((value shr 16 and 0xFF) / 255.0f)
                byteBuffer.putFloat((value shr 8 and 0xFF) / 255.0f)
                byteBuffer.putFloat((value and 0xFF) / 255.0f)
            }
        }
        return byteBuffer
    }

    private fun getDetections(byteBuffer: ByteBuffer, bitmap: Bitmap): ArrayList<Classifier.Recognition> {
        val detections: ArrayList<Classifier.Recognition> = ArrayList()
        val outputMap: MutableMap<Int, Any> = HashMap()
        for (i in OUTPUT_WIDTH.indices) {
            val out = Array(1) {
                Array(
                    OUTPUT_WIDTH[i]
                ) {
                    Array(
                        OUTPUT_WIDTH[i]
                    ) {
                        Array(3) {
                            FloatArray(5 + labels.size)
                        }
                    }
                }
            }
            outputMap[i] = out
        }
        Log.d("YoloV4Classifier", "mObjThresh: $objThresh")
        val inputArray = arrayOf<Any>(byteBuffer)
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        for (i in OUTPUT_WIDTH.indices) {
            val gridWidth = OUTPUT_WIDTH[i]
            val out = outputMap[i] as Array<Array<Array<Array<FloatArray>>>>?
            Log.d("YoloV4Classifier", "out[$i] detect start")
            for (y in 0 until gridWidth) {
                for (x in 0 until gridWidth) {
                    for (b in 0 until NUM_BOXES_PER_BLOCK) {
                        val offset =
                            gridWidth * (NUM_BOXES_PER_BLOCK * (labels.size + 5)) * y + NUM_BOXES_PER_BLOCK * (labels.size + 5) * x + (labels.size + 5) * b
                        val confidence: Float = expit(out!![0][y][x][b][4])
                        var detectedClass = -1
                        var maxClass = 0f
                        val classes = FloatArray(labels.size)
                        for (c in labels.indices) {
                            classes[c] = out[0][y][x][b][5 + c]
                        }
                        for (c in labels.indices) {
                            if (classes[c] > maxClass) {
                                detectedClass = c
                                maxClass = classes[c]
                            }
                        }
                        val confidenceInClass = maxClass * confidence
                        if (confidenceInClass > objThresh) {
                            val xPos: Float =
                                (x + expit(out[0][y][x][b][0])) * (1.0f * INPUT_SIZE / gridWidth)
                            val yPos: Float =
                                (y + expit(out[0][y][x][b][1])) * (1.0f * INPUT_SIZE / gridWidth)
                            val w =
                                (exp(out[0][y][x][b][2].toDouble()) * ANCHORS[2 * MASKS[i][b]]).toFloat()
                            val h =
                                (exp(out[0][y][x][b][3].toDouble()) * ANCHORS[2 * MASKS[i][b] + 1]).toFloat()
                            val rect = RectF(
                                0f.coerceAtLeast(xPos - w / 2),
                                0f.coerceAtLeast(yPos - h / 2),
                                (bitmap.width - 1.toFloat()).coerceAtMost(xPos + w / 2),
                                (bitmap.height - 1.toFloat()).coerceAtMost(yPos + h / 2)
                            )
                            detections.add(
                                Classifier.Recognition(
                                    "" + offset, labels[detectedClass],
                                    confidenceInClass, rect, detectedClass
                                )
                            )
                        }
                    }
                }
            }
            Log.d("YoloV4Classifier", "out[$i] detect end")
        }
        return detections
    }

    /**
     * For yolov4-tiny, the situation would be a little different from the yolov4, it only has two
     * output. Both has three dimenstion. The first one is a tensor with dimension [1, 2535,4], containing all the bounding boxes.
     * The second one is a tensor with dimension [1, 2535, class_num], containing all the classes score.
     * @param byteBuffer input ByteBuffer, which contains the image information
     * @param bitmap pixel disenty used to resize the output images
     * @return an array list containing the recognitions
     */
    private fun getDetectionsForTiny(
        byteBuffer: ByteBuffer,
        bitmap: Bitmap
    ): ArrayList<Classifier.Recognition> {
        val detections: ArrayList<Classifier.Recognition> = ArrayList()
        val outputMap: MutableMap<Int, Any> = HashMap()
        outputMap[0] = Array(1) {
            Array(OUTPUT_WIDTH_TINY[0]) {
                FloatArray(
                    4
                )
            }
        }
        outputMap[1] = Array(1) {
            Array(OUTPUT_WIDTH_TINY[1]) {
                FloatArray(
                    labels.size
                )
            }
        }
        val inputArray = arrayOf<Any>(byteBuffer)
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        val gridWidth = OUTPUT_WIDTH_TINY[0]
        val bboxes = outputMap[0] as Array<Array<FloatArray>>?
        val outScore = outputMap[1] as Array<Array<FloatArray>>?
        for (i in 0 until gridWidth) {
            var maxClass = 0f
            var detectedClass = -1
            val classes = FloatArray(labels.size)
            for (c in labels.indices) {
                classes[c] = outScore!![0][i][c]
            }
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }
            val score = maxClass
            if (score > objThresh) {
                val xPos = bboxes!![0][i][0]
                val yPos = bboxes[0][i][1]
                val w = bboxes[0][i][2]
                val h = bboxes[0][i][3]
                val rectF = RectF(
                    0f.coerceAtLeast(xPos - w / 2),
                    0f.coerceAtLeast(yPos - h / 2),
                    (bitmap.width - 1.toFloat()).coerceAtMost(xPos + w / 2),
                    (bitmap.height - 1.toFloat()).coerceAtMost(yPos + h / 2)
                )
                detections.add(
                    Classifier.Recognition(
                        "" + i,
                        labels[detectedClass], score, rectF, detectedClass
                    )
                )
            }
        }
        return detections
    }

    override fun recognizeImage(bitmap: Bitmap?): ArrayList<Classifier.Recognition?>? {
        val byteBuffer = convertBitmapToByteBuffer(bitmap!!)
        //check whether the tiny version is specified
        val detections: ArrayList<Classifier.Recognition> = if (isTiny) {
            getDetectionsForTiny(byteBuffer, bitmap)
        } else {
            getDetections(byteBuffer, bitmap)
        }
        return nms(detections)
    }

    companion object {
        /**
         * Initializes a native TensorFlow session for classifying images.
         *
         * @param assetManager  The asset manager to be used to load assets.
         * @param modelFilename The filepath of the model GraphDef protocol buffer.
         * @param labelFilename The filepath of label file for classes.
         * @param isQuantized   Boolean representing model is quantized or not
         */
        fun create(
            assetManager: AssetManager,
            modelFilename: String?,
            labelFilename: String,
            isQuantized: Boolean
        ): Classifier {
            val d = YoloV4Classifier()
            val actualFilename = labelFilename.split("file:///android_asset/").toTypedArray()[1]
            val labelsInput = assetManager.open(actualFilename)
            val br = BufferedReader(InputStreamReader(labelsInput))

            br.forEachLine {
                LOGGER.w(it)
                d.labels.add(it)
            }

            br.close()
            try {
                val options = Interpreter.Options()
                options.setNumThreads(NUM_THREADS)
                if (isNNAPI) {
                    val nnApiDelegate: NnApiDelegate?
                    // Initialize interpreter with NNAPI delegate for Android Pie or above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        nnApiDelegate = NnApiDelegate()
                        options.addDelegate(nnApiDelegate)
                        options.setNumThreads(NUM_THREADS)
                        options.setUseNNAPI(false)
                        options.setAllowFp16PrecisionForFp32(true)
                        options.setAllowBufferHandleOutput(true)
                        options.setUseNNAPI(true)
                    }
                }
                if (isGPU) {
                    val gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                }
                d.tfLite = Interpreter(Utils.loadModelFile(assetManager, modelFilename), options)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            d.isModelQuantized = isQuantized
            // Pre-allocate buffers.
            val numBytesPerChannel: Int = if (isQuantized) {
                1 // Quantized
            } else {
                4 // Floating point
            }
            val allocateDirect = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * numBytesPerChannel)
            allocateDirect.order(ByteOrder.nativeOrder())
            d.imgData = allocateDirect
            d.intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            return d
        }

        private val LOGGER: Logger = Logger()

        //config yolov4
        private const val INPUT_SIZE = 640
        private val OUTPUT_WIDTH = intArrayOf(52, 26, 13)
        private val MASKS = arrayOf(intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8))
        private val ANCHORS = intArrayOf(
            12, 16, 19, 36, 40, 28, 36, 75, 76, 55, 72, 146, 142, 110, 192, 243, 459, 401
        )
        private const val NUM_BOXES_PER_BLOCK = 3

        // Number of threads in the java app
        private const val NUM_THREADS = 4
        private const val isNNAPI = false
        private const val isGPU = true

        // tiny or not
        private const val isTiny = true

        // config yolov4 tiny
        private val OUTPUT_WIDTH_TINY = intArrayOf(6000, 6000)
        protected const val BATCH_SIZE = 1
        protected const val PIXEL_SIZE = 3
    }
}