package si.blarc

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import si.blarc.env.TF_OD_API_INPUT_SIZE
import si.blarc.env.Utils.getBitmapFromAsset
import si.blarc.env.Utils.processBitmap
import si.blarc.tflite.Classifier

class MainActivity : AppCompatActivity() {

    private lateinit var detectButton: Button
    private lateinit var imageView: ImageView
    private lateinit var scoreText: TextView

    private lateinit var sourceBitmap: Bitmap
    private lateinit var croppedBitmap: Bitmap

    private lateinit var detector: KingdominoDetector
    private var detectionDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        detectButton = findViewById(R.id.main_detect_button)
        detectButton.setOnClickListener { onDetectButtonClick() }
//        TODO @jakobm
//        detectButton.setOnClickListener { startActivity(Intent(this, CameraActivity::class.java)) }

        imageView = findViewById(R.id.main_image_view)
        scoreText = findViewById(R.id.main_score_text)

        sourceBitmap = getBitmapFromAsset(this, "test2.jpg") ?:
                throw Exception("Image file not found!")

        croppedBitmap = processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE)
        imageView.setImageBitmap(croppedBitmap)

        detector = KingdominoDetector(applicationContext, 5)
    }

    private fun detectObjects() : Single<Array<Array<Classifier.Recognition?>>> {
        return Single.create { emitter ->
            emitter.onSuccess(detector.detectObjects(croppedBitmap))
        }
    }

    private fun onDetectButtonClick() {
        detectionDisposable = detectObjects()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onSuccess = {
                            handleResult(it)
                            if (detectionDisposable != null) {
                                detectionDisposable!!.dispose()
                            }
                        }
                )
    }

    private fun handleResult(result: Array<Array<Classifier.Recognition?>>) {
        // TODO @blarc Use resources instead.
        scoreText.text = "%d points".format(0)
        imageView.setImageBitmap(croppedBitmap)
    }
}