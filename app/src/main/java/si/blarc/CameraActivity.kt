package si.blarc

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.camerakit.CameraKitView

/***
 * Activity that opens camera for taking a picture of the board.
 * @author blarc
 */
class CameraActivity : AppCompatActivity() {
    private lateinit var cameraKitView: CameraKitView
    private lateinit var cameraButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraKitView = findViewById(R.id.camera)
        cameraButton = findViewById(R.id.camera_button)

        cameraButton.setOnClickListener { cameraKitView.captureImage { _: CameraKitView, capturedImage: ByteArray ->
            val imageBitmap = BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage.size)
        }
        }

    }


    override fun onStart() {
        super.onStart()
        cameraKitView.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView.onResume()
    }

    override fun onPause() {
        cameraKitView.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}