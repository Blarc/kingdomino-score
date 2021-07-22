package si.blarc

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import si.blarc.enum.TileEnum
import si.blarc.env.*
import si.blarc.tflite.Classifier
import si.blarc.tflite.YoloV4Classifier
import java.io.IOException
import kotlin.math.abs

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
     * Crops the original image and calls [detectObjects] function.
     * @param [sourceBitmap] A bitmap image from which we want to detect the objects.
     * @return Array of detected objects.
     * @author blarc
     */
    suspend fun findObjects(sourceBitmap: Bitmap) : Bitmap {
        return withContext(Dispatchers.IO) {
            val croppedBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE)
            detectObjects(croppedBitmap, sourceBitmap)
            return@withContext croppedBitmap
        }
    }

    /**
     * Detects objects and orders them into a N * N matrix that presents the board state.
     * @param [image] A cropped bitmap image from which we want to detect the objects.
     * @return A matrix of [n] * [n] size that holds detected objects ordered as in the picture.
     * @author blarc
     */
    private fun detectObjects(image: Bitmap, sourceBitmap: Bitmap) : List<Classifier.Recognition> {
        val detectedObjects = classifier.recognizeImage(image)!!
        logger.i(detectedObjects.size.toString())
        for (detectedObject in detectedObjects) {
            logger.i(detectedObject.title.toString())
        }

        val xRatio = image.width / sourceBitmap.width.toFloat()
        val yRatio = image.height / sourceBitmap.height.toFloat()

        val centerTile = detectedObjects.find { it.detectedClass == TileEnum.CENTER }!!
        setAllNeighbours(centerTile, detectedObjects, xRatio, yRatio)

        return detectedObjects
    }

    private fun setAllNeighbours(tile: Classifier.Recognition, objects: List<Classifier.Recognition>, xRatio: Float, yRatio: Float) {
        if (tile.neighbours.isEmpty()) {
            print("Searching neighbours for ${tile.title} (${tile.getLocation().centerX()} ${tile.getLocation().centerY()}):\n")
            tile.neighbours = findNeighbours(tile, objects, xRatio, yRatio)
            for (neighbour: Classifier.Recognition in tile.neighbours) {
                print("${neighbour.title} (${neighbour.getLocation().centerX()} ${neighbour.getLocation().centerY()} ${tile.manhattanDistance(neighbour, xRatio, yRatio)}) ")
            }
            print("\n")

            for (neighbour: Classifier.Recognition in tile.neighbours) {
                setAllNeighbours(neighbour, objects, xRatio, yRatio)
            }
        }
    }

    private fun findNeighbours(tile: Classifier.Recognition, objects: List<Classifier.Recognition>, xRatio: Float, yRatio: Float): List<Classifier.Recognition> {
        val sortedObjects = objects.sortedWith(compareBy { it.manhattanDistance(tile, xRatio, yRatio)} )
        val neighbours = sortedObjects.subList(1, 5)

        val closestDistance = tile.manhattanDistance(neighbours[0], xRatio, yRatio)

        neighbours.forEachIndexed { i, neighbour ->
            if (abs(tile.manhattanDistance(neighbour, xRatio, yRatio) - closestDistance) > 5) {
                return neighbours.subList(0, i)
            }
        }
        return neighbours
    }
}