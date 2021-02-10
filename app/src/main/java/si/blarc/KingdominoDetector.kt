package si.blarc

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import si.blarc.env.Logger
import si.blarc.env.TF_OD_API_IS_QUANTIZED
import si.blarc.env.TF_OD_API_LABELS_FILE
import si.blarc.env.TF_OD_API_MODEL_FILE
import si.blarc.tflite.Classifier
import si.blarc.tflite.YoloV4Classifier
import java.io.IOException

/***
 * A wrapper class for board detection.
 * @param [context] Application context
 * @property [n] The size of one row/column of the board (5 or 7). The board can be (5x5 or 7x7)
 * @constructor Initializes a classifier from specified parameters.
 * @author blarc
 */
class KingdominoDetector(context: Context, private var n: Int) {
    private val logger: Logger = Logger()

    private lateinit var classifier: Classifier

    /***
     * Initializes classifier from the model specified in Config.TF_OD_API_MODEL_FILE,
     * labels specified in Config.TF_OD_API_LABELS_FILE.
     */
    init {
        try {
            classifier = YoloV4Classifier.create(
                    context.assets,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_IS_QUANTIZED
            )
        }
        catch (e: IOException) {
            e.printStackTrace()
            logger.e(e, "Exception initializing classifier!")
            val toast = Toast.makeText(
                    context,
                    "Classifier could not be initialized",
                    Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

    /***
     * Detects objects and orders them into a N * N matrix that presents the board state.
     * @param [image] A bitmap image from which we want to detect the objects.
     * @return A matrix of [n] * [n] size that holds detected objects ordered as in the picture.
     * @author blarc
     */
    fun detectObjects(image: Bitmap) : Array<Array<Classifier.Recognition?>> {
        var detectedObjects = classifier.recognizeImage(image)

        val board = Array(n) { arrayOfNulls<Classifier.Recognition>(n) }
//        FIXME @blarc Won't work for empty spaces.
//        if (detectedObjects != null) {
//            detectedObjects = detectedObjects.sortedWith(compareBy { it?.getLocation()?.centerY() })
//
//            for(i in 0 until n) {
//                for(j in 0 until n) {
//                    board[i][j] = detectedObjects[i*n+j]!!
//                }
//                board[i].sortBy { it?.getLocation()?.centerX() }
//            }
//        } else {
//            logger.w("Zero objects detected!")
//        }

        logger.i(detectedObjects?.size.toString())
        if (detectedObjects != null) {
            for (detectedObject in detectedObjects) {
                logger.i(detectedObject?.title.toString())
            }
        }

        return board
    }
}