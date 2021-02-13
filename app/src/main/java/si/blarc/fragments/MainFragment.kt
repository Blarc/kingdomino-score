package si.blarc.fragments

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import si.blarc.KingdominoDetector
import si.blarc.R
import si.blarc.env.Constants
import si.blarc.env.TF_OD_API_INPUT_SIZE
import si.blarc.env.UIUtils
import si.blarc.env.Utils
import si.blarc.tflite.Classifier

/***
 * This is the first fragment a user sees. In this fragment user can choose offline or online mode.
 * @author blarc
 */
class MainFragment : Fragment() {
    private lateinit var imageView: ImageView
    private lateinit var scanBtn: Button
    private lateinit var scoreText: TextView
    private lateinit var detector: KingdominoDetector

    private lateinit var imageUri: Uri

    private var detectionDisposable: Disposable? = null

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.main_image_view)
        scanBtn = view.findViewById(R.id.main_scan_btn)
        scoreText = view.findViewById(R.id.main_score_text_view)
        detector = KingdominoDetector(requireContext(), 5)

        scanBtn.setOnClickListener {
            UIUtils.checkPermissionFragment(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                    Constants.MY_PERMISSIONS_MULTIPLE
            ) {
                prepareImageFile()
                startCameraActivity()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.CAMERA_REQUEST_CODE) {
            try {
                val contentResolver = requireContext().contentResolver
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                val bitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)

                imageView.setImageBitmap(bitmap)

                detectionDisposable = detector.findObjects(bitmap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                                onSuccess = {
                                    handleResult(it)
                                },
                                onError = {
                                    handleError(it)
                                }
                        )

                contentResolver.delete(imageUri, null, null)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startCameraActivity() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, Constants.CAMERA_REQUEST_CODE)
    }

    private fun prepareImageFile() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, Constants.TEMP_FILE_NAME)
        imageUri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        )!!
    }

    private fun handleResult(results: Array<Array<Classifier.Recognition?>>) {
        // TODO @blarc Use resources instead.
        scoreText.text = "%d points".format(0)
    }

    private fun handleError(it: Throwable) {
        TODO("Not yet implemented")
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.MY_PERMISSIONS_MULTIPLE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prepareImageFile()
                    startCameraActivity()
                } else {
                    Toast.makeText(requireContext(), "Write external storage denied!", Toast.LENGTH_SHORT).show()
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



}