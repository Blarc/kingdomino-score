package si.blarc

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import si.blarc.env.Utils.getBitmapFromAsset
import si.blarc.env.Utils.processBitmap
import si.blarc.tflite.Classifier
import si.blarc.tflite.YoloV4Classifier
import si.blarc.env.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val logger: Logger = Logger()

    private lateinit var detectButton: Button
    private lateinit var imageView: ImageView

    private lateinit var sourceBitmap: Bitmap
    private lateinit var croppedBitmap: Bitmap

    private lateinit var classifier: Classifier;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        detectButton = findViewById(R.id.detectButton)
        detectButton.setOnClickListener {
            // TODO @blarc Handler is deprecated.
            val handler = Handler()
            Thread {
                val results = classifier.recognizeImage(croppedBitmap)
                handler.post { handleResult(croppedBitmap, results) }
            }.start()
        }

        imageView = findViewById(R.id.imageView)

        sourceBitmap = getBitmapFromAsset(this, "test2.jpg") ?:
                throw Exception("Image file not found!")

        croppedBitmap = processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE)
        imageView.setImageBitmap(croppedBitmap)

        initClassifier()

    }

    /***
     * Initializes classifier from the model specified in Config.TF_OD_API_MODEL_FILE,
     * labels specified in Config.TF_OD_API_LABELS_FILE.
     */
    private fun initClassifier() {
        try {
            classifier = YoloV4Classifier.create(
                    assets,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_IS_QUANTIZED
            )
        }
        catch (e: IOException) {
            e.printStackTrace()
            logger.e(e, "Exception initializing classifier!")
            val toast = Toast.makeText(
                    applicationContext,
                    "Classifier could not be initialized",
                    Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
    }

    private fun handleResult(croppedBitmap: Bitmap, results: List<Classifier.Recognition?>?) {

        if (results != null && results.isNotEmpty()) {
            for (result in results) {
                Log.i("Result:", result.toString())
            }
        }

        imageView.setImageBitmap(croppedBitmap)
    }
}