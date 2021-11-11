//package com.example.jetpackcamerax
//
//import android.content.Context
//import android.content.pm.PackageManager
//import android.icu.text.SimpleDateFormat
//import android.media.Image
//import android.net.Uri
//import android.os.Build
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.annotation.RequiresApi
//import androidx.camera.core.*
//import androidx.camera.extensions.ExtensionMode
//import androidx.camera.extensions.ExtensionsManager
//
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import kotlinx.android.synthetic.main.activity_main.*
//import java.io.File
//import java.nio.ByteBuffer
//import java.security.cert.Extension
//import java.util.*
//import java.util.concurrent.Executor
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import java.util.jar.Manifest
//
//
//
//typealias LumaListener = (luma: Double) -> Unit
//
//class MainActivity : AppCompatActivity() {
//    private var imageCapture: ImageCapture? = null
//
//    private lateinit var outputDirectory: File
//    private lateinit var cameraExecutor: ExecutorService
//
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//
//
//        //Request camera permissions
//        if (allPermissionsGranted()) {
//            startCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSIONS
//            )
//        }
//        camera_capture_button.setOnClickListener { takePhoto() }
//
//        outputDirectory = getOutputDirectory()
//
//        cameraExecutor = Executors.newSingleThreadExecutor()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.N)
//    private fun takePhoto() {
//        val imageCapture = imageCapture ?: return
//        // create time stamped output file to hold the image
//        val photoFile = File(
//            outputDirectory,
//            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
//        )
//        // create output options object which contain file + meta data
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                    val savedUri = Uri.fromFile(photoFile)
//                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
//                }
//
//            }
//        )
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        val extensionsManager = ExtensionsManager.getInstance(this)
//
//
//        cameraProviderFuture.addListener(Runnable {
//            // Create an extensions manager
//
//
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            //Preview
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
//
//            imageCapture = ImageCapture.Builder().build()
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                        Log.d(TAG, "Average luminosity: $luma")
//                    })
//                }
//            //select back camera as derault
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            Log.e("MainActivity isExtensionAvailable", "=====" + extensionsManager.get().isExtensionAvailable(cameraProvider, cameraSelector, ExtensionMode.BOKEH));
//
//            val bokehCameraSelector = extensionsManager.get().getExtensionEnabledCameraSelector(
//                cameraProvider,
//                cameraSelector,
//                ExtensionMode.BOKEH
//            )
//
//
//
//            try {
//
//                    //Unbind use cases before rebinding
//                    cameraProvider.unbindAll()
//                    //Bind use cases to camera
//
//                    cameraProvider.bindToLifecycle(
//                        this,
//                        bokehCameraSelector,
//                        imageCapture,
//                        preview,
//                        imageAnalyzer
//                    )
//
//            } catch (exc: Exception) {
//                Log.e(TAG, " use case binding failed", exc)
//            }
//        }, ContextCompat.getMainExecutor(this))
//
//    }
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all {
//        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
//    }
//
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    private fun getOutputDirectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(R.string.app_name)).apply {
//                mkdirs()
//            }
//        }
//        return if (mediaDir != null && mediaDir.exists())
//            mediaDir else filesDir
//
//    }
//
//    companion object {
//        private const val TAG = "CameraXBasic"
//        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        private val REQUIRED_PERMISSION = arrayOf(android.Manifest.permission.CAMERA)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            if (allPermissionsGranted()) {
//                startCamera()
//            } else {
//                Toast.makeText(this, "Permission not granted by user.", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//        }
//    }
//
//    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
//
//        private fun ByteBuffer.toByteArray(): ByteArray {
//            rewind() // rewind the buffer to zero
//            val data = ByteArray(remaining())
//            get(data) //Copy the buffer into a byte array
//            return data // return the byte array
//        }
//
//        override fun analyze(image: ImageProxy) {
//
//            val buffer = image.planes[0].buffer
//            val data = buffer.toByteArray()
//            val pixels = data.map { it.toInt() and 0xFF }
//            val luma = pixels.average()
//
//            listener(luma)
//
//            image.close()
//        }
//
//    }
//}