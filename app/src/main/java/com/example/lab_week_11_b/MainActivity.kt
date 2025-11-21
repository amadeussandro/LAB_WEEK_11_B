package com.example.lab_week_11_b

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 3
    }

    // File manager
    private lateinit var providerFileManager: ProviderFileManager

    // Data model untuk foto & video
    private var photoInfo: FileInfo? = null
    private var videoInfo: FileInfo? = null

    // Flag untuk mengecek apakah user record video
    private var isCapturingVideo = false

    // Launcher kamera
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi ProviderFileManager
        providerFileManager = ProviderFileManager(
            applicationContext,
            FileHelper(applicationContext),
            contentResolver,
            Executors.newSingleThreadExecutor(),
            MediaContentHelper()
        )

        // Launcher FOTO
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) {
            providerFileManager.insertImageToStore(photoInfo)
            Toast.makeText(this, "Photo saved!", Toast.LENGTH_SHORT).show()
        }

        // Launcher VIDEO
        takeVideoLauncher = registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) {
            providerFileManager.insertVideoToStore(videoInfo)
            Toast.makeText(this, "Video saved!", Toast.LENGTH_SHORT).show()
        }

        // Tombol FOTO
        findViewById<Button>(R.id.photo_button).setOnClickListener {
            isCapturingVideo = false
            checkStoragePermission {
                openImageCapture()
            }
        }

        // Tombol VIDEO
        findViewById<Button>(R.id.video_button).setOnClickListener {
            isCapturingVideo = true
            checkStoragePermission {
                openVideoCapture()
            }
        }
    }

    // Buka kamera → FOTO
    private fun openImageCapture() {
        photoInfo = providerFileManager.generatePhotoUri(System.currentTimeMillis())
        takePictureLauncher.launch(photoInfo!!.uri)   // FIX: Uri non-null
    }

    // Buka kamera → VIDEO
    private fun openVideoCapture() {
        videoInfo = providerFileManager.generateVideoUri(System.currentTimeMillis())
        takeVideoLauncher.launch(videoInfo!!.uri)     // FIX: Uri non-null
    }

    // Permission → Android 9 kebawah butuh WRITE_EXTERNAL_STORAGE
    private fun checkStoragePermission(onPermissionGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {

            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
                PackageManager.PERMISSION_GRANTED -> onPermissionGranted()

                else -> ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_EXTERNAL_STORAGE
                )
            }

        } else {
            // Android 10+ tidak perlu permission
            onPermissionGranted()
        }
    }

    // Handle hasil permission Android 9 kebawah
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (isCapturingVideo) {
                        openVideoCapture()
                    } else {
                        openImageCapture()
                    }
                }
            }
        }
    }
}
