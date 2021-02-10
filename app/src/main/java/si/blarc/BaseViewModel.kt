package si.blarc

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import si.blarc.env.TF_OD_API_INPUT_SIZE
import si.blarc.env.Utils.processBitmap
import si.blarc.tflite.Classifier

/***
 * Base activity's view model that is used for communication between fragments.
 * @author blarc
 */
class BaseViewModel(application: Application) : AndroidViewModel(application) {
    var scannedImage = MutableLiveData<Bitmap>()
    private val detector = KingdominoDetector(application, 5)

    fun detectObjects(sourceBitmap: Bitmap) : Single<Array<Array<Classifier.Recognition?>>> {
        return Single.create { emitter ->
            val croppedBitmap = processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE)
            val detectedObjects = detector.detectObjects(croppedBitmap)
            emitter.onSuccess(detectedObjects)
        }
    }

}
