import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var imageCapture: ImageCapture
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startCamera()
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                takePhotoAndSend()
            }
        }, 0, 4000)
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
                sendImage(file)
            }
            override fun onError(exception: ImageCaptureException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun sendImage(file: File) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("http://192.168.1.12:5000/upload") // Ganti dengan IP komputer
            .post(requestBody)
            .build()
        client.newCall(request).execute()
    }
}