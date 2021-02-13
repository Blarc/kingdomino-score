package si.blarc.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.reactivex.Single
import si.blarc.KingdominoDetector
import si.blarc.entity.Player
import si.blarc.env.Config.TF_OD_API_INPUT_SIZE
import si.blarc.env.Utils.processBitmap
import si.blarc.tflite.Classifier

/***
 * Base activity's view model that is used for communication between fragments.
 * @author blarc
 */
class BaseViewModel(application: Application) : AndroidViewModel(application) {
    private val detector = KingdominoDetector(application, 5)
    var auth = Firebase.auth
    var database = Firebase.database.reference

    var currentPlayer : Player? = null
    var gameId: String? = null

    var scannedImage = MutableLiveData<Bitmap>()
    var players = MutableLiveData<MutableList<Player>>(mutableListOf())

    fun detectObjects(sourceBitmap: Bitmap) : Single<Array<Array<Classifier.Recognition?>>> {
        return Single.create { emitter ->
            val croppedBitmap = processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE)
            val detectedObjects = detector.detectObjects(croppedBitmap)
            emitter.onSuccess(detectedObjects)
        }
    }

    fun addPlayer(player: Player) {
        val players = players.value
        players?.add(player)
        this.players.value = players
    }

    fun getPlayers() : ArrayList<Player> {
        val players = players.value
        if (players != null) {
            return ArrayList(players)
        }
        return ArrayList()
    }

    fun reset() {
        currentPlayer = null
        gameId = null
        players.value = mutableListOf()
    }

}
