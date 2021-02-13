package si.blarc.ui.fragments

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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import si.blarc.ui.BaseViewModel
import si.blarc.R
import si.blarc.env.Constants
import si.blarc.env.Constants.MY_PERMISSIONS_MULTIPLE
import si.blarc.env.Constants.TEMP_FILE_NAME
import si.blarc.env.UIUtils.checkPermissionFragment
import si.blarc.env.UIUtils.replaceFragment

/***
 * This fragment shows current leaderboard and players' scores.
 * @author blarc
 */
class LeaderboardFragment : Fragment() {

    private lateinit var scanBtn: Button
    private lateinit var imageUri: Uri

    private val baseViewModel: BaseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanBtn = view.findViewById(R.id.leaderboard_scan_btn)
        scanBtn.setOnClickListener {
            checkPermissionFragment(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA),
                MY_PERMISSIONS_MULTIPLE
            ) {
                prepareImageFile()
                startCameraActivity()
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
        contentValues.put(MediaStore.Images.Media.TITLE, TEMP_FILE_NAME)
        imageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.CAMERA_REQUEST_CODE) {
            try {
                val contentResolver = requireContext().contentResolver
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                val bitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                baseViewModel.scannedImage.value = bitmap
                contentResolver.delete(imageUri, null, null)

            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            replaceFragment(requireActivity(), ScoreFragment::class.java)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_MULTIPLE ->
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