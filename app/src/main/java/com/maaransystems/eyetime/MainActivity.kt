package com.maaransystems.eyetime
import android.speech.tts.TextToSpeech
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var blinkCounterTextView: TextView
    private var blinkCount = 0
    var eyesClosed = false

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
    private lateinit var tts: TextToSpeech
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        blinkCounterTextView = findViewById(R.id.blinkCountText)

        // Request camera permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            startCamera()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.UK
            speak("Hello! Please blink your eyes 10 times to keep them healthy.")
        }
    }
    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Image Analyzer for Frame Processing
            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
                    processImage(imageProxy) // Custom method for face detection
                })
            }

            // Bind everything to lifecycle
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Set up face detector options
            val options = FaceDetectorOptions.Builder()
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            // Get an instance of ML Kit's face detector
            val detector = FaceDetection.getClient(options)

            // Process the image
            detector.process(image)
                .addOnSuccessListener { faces ->
                    detectBlink(faces)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close() // Always close the imageProxy when done
                }
        }
    }


    fun detectBlink(faces: List<Face>) {
        for (face in faces) {
            // Probabilities for the eyes being open
            val leftEyeOpenProb = face.leftEyeOpenProbability ?: -1.0f
            val rightEyeOpenProb = face.rightEyeOpenProbability ?: -1.0f

            // Check if both eyes are closed
            if (leftEyeOpenProb < 0.5 && rightEyeOpenProb < 0.5) {
                eyesClosed = true
            }
            // Check if the eyes are open after being closed (blink detection)
            else if (eyesClosed && leftEyeOpenProb > 0.5 && rightEyeOpenProb > 0.5) {
                blinkCount++
                eyesClosed = false
                //Toast.makeText(applicationContext,"$blinkCount time blinked out of 10",Toast.LENGTH_SHORT).show()
                blinkCounterTextView.text = "Blinks: $blinkCount"
                speak("$blinkCount time blinked")
                if (blinkCount == 10){
                    speak("Great You finished your eyetime,Thank you")
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        cameraExecutor.shutdown()
    }
    override fun onPause() {
        super.onPause()
        // Delay for 1 minute (60,000 milliseconds)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            // Bring the app to the foreground
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }, 60000) // 60,000 milliseconds = 1 minute
    }
}
