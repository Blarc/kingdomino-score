package si.blarc.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import si.blarc.ui.BaseViewModel
import si.blarc.KingdominoDetector
import si.blarc.R
import si.blarc.tflite.Classifier
import kotlin.random.Random

/***
 * This fragment calculates the points of the scanned board and shows them to the player.
 * @author blarc
 */
class ScoreFragment : Fragment() {

    private val baseViewModel: BaseViewModel by activityViewModels()

    private lateinit var imageView: ImageView
    private lateinit var scoreText: TextView
    private lateinit var detector: KingdominoDetector

    private var detectionDisposable: Disposable? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_score, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageView = view.findViewById(R.id.score_image_view)
        scoreText = view.findViewById(R.id.score_text_view)

        val scannedImage = baseViewModel.scannedImage.value
        imageView.setImageBitmap(scannedImage)
        detector = KingdominoDetector(requireContext(), 5)

        detectionDisposable = baseViewModel.detectObjects(scannedImage!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    handleResult(it)
                }
            )
    }

    private fun handleResult(results: Array<Array<Classifier.Recognition?>>) {
        // TODO @blarc Use resources instead.
        scoreText.text = "%d points".format(Random.nextInt())
    }
}