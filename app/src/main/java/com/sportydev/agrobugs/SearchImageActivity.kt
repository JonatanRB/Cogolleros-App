package com.sportydev.agrobugs

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class SearchImageActivity : BaseActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var yoloDetector: YoloDetector
    private lateinit var adminBd: AdminBd

    // Launcher para galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImageFromGallery(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_image)

        // Inicializar detector y BD
        try {
            yoloDetector = YoloDetector(this)
            adminBd = AdminBd(this)

            // Debug: Imprimir plagas en la BD
            adminBd.printAllPests()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al inicializar: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        checkCameraPermissionAndStart()
        setupActionButtons()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupActionButtons() {
        findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.btnShutter).setOnClickListener {
            takePhoto()
        }

        // Botón de galería
        findViewById<android.view.View>(R.id.btnGallery).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.cameraPreview).surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Fallo al vincular los casos de uso", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AgroBugs")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error al guardar la foto: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Error al guardar la foto.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        processImageAndDetect(uri)
                    }
                }
            }
        )
    }

    private fun processImageFromGallery(uri: Uri) {
        processImageAndDetect(uri)
    }

    private fun processImageAndDetect(uri: Uri) {
        try {
            // Mostrar diálogo de procesamiento
            val progressDialog = AlertDialog.Builder(this)
                .setTitle("Analizando imagen...")
                .setMessage("Detectando plagas, por favor espera...")
                .setCancelable(false)
                .create()
            progressDialog.show()

            // Cargar imagen en background
            cameraExecutor.execute {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        // Ejecutar detección
                        val detections = yoloDetector.detect(bitmap)

                        runOnUiThread {
                            progressDialog.dismiss()

                            if (detections.isNotEmpty()) {
                                // Tomar la detección con mayor confianza
                                val bestDetection = detections.maxByOrNull { it.confidence }!!

                                Log.d(
                                    "Detection",
                                    "Detectado: ${bestDetection.label} (${bestDetection.confidence})"
                                )

                                // Buscar en la base de datos
                                val pest =
                                    PestMapper.findPestInDatabase(bestDetection.label, adminBd)

                                if (pest != null) {
                                    // ✅ ABRIR DIRECTAMENTE InformationBugActivity
                                    openBugInformation(
                                        pest.idPlaga,
                                        bestDetection.label,
                                        bestDetection.confidence
                                    )
                                } else {
                                    // No se encontró en la BD, mostrar error y sugerencias
                                    showPestNotFoundDialog(bestDetection.label, detections)
                                }
                            } else {
                                Toast.makeText(
                                    this,
                                    "❌ No se detectaron plagas en la imagen.\nIntenta tomar otra foto.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error al procesar: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPestNotFoundDialog(detectedLabel: String, allDetections: List<Detection>) {
        val message = buildString {
            append("🔍 Plaga detectada: $detectedLabel\n\n")
            append("⚠️ Esta plaga no está registrada en la base de datos.\n\n")

            if (allDetections.size > 1) {
                append("Otras detecciones:\n")
                allDetections.drop(1).take(3).forEach { detection ->
                    append("• ${detection.label} (${String.format("%.1f%%", detection.confidence * 100)})\n")
                }
                append("\n")
            }

            append("💡 Verifica que '$detectedLabel' esté en la base de datos o actualiza el archivo labels.txt")
        }

        AlertDialog.Builder(this)
            .setTitle("Plaga No Registrada")
            .setMessage(message)
            .setPositiveButton("Ver BD") { _, _ ->
                showAllPestsInDatabase()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showDetectionsDialog(detections: List<Detection>) {
        val message = buildString {
            append("Plagas detectadas:\n\n")
            detections.forEachIndexed { index, detection ->
                append("${index + 1}. ${detection.label}\n")
                append("   Confianza: ${String.format("%.2f%%", detection.confidence * 100)}\n\n")
            }
            append("⚠ No se encontró información en la base de datos.\n")
            append("Verifica que el nombre '${detections.first().label}' esté registrado.")
        }

        AlertDialog.Builder(this)
            .setTitle("Resultado de Detección")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Ver todas las plagas") { _, _ ->
                showAllPestsInDatabase()
            }
            .show()
    }

    private fun showAllPestsInDatabase() {
        val allPests = adminBd.getAllPests()
        val pestNames = allPests.joinToString("\n") { "• ${it.nomPlaga} (${it.nomcientifico})" }

        AlertDialog.Builder(this)
            .setTitle("Plagas en la Base de Datos")
            .setMessage(if (pestNames.isNotEmpty()) pestNames else "No hay plagas registradas")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openBugInformation(pestId: Int, detectedName: String, confidence: Float) {
        val intent = Intent(this, InformationBugActivity::class.java)
        intent.putExtra(InformationBugActivity.EXTRA_PLAGA_ID, pestId)
        intent.putExtra("detected_name", detectedName)
        intent.putExtra("confidence", confidence)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        yoloDetector.close()
    }
}