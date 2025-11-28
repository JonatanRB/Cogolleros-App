package com.sportydev.agrobugs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)

class YoloDetector (private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        private const val MODEL_PATH = "models/best_plagas32.tflite"
        private const val LABELS_PATH = "models/labels.txt"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.25f
        private const val IOU_THRESHOLD = 0.45f
    }

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_PATH)
            interpreter = Interpreter(modelBuffer, options)

            println("✓ Modelo YOLO cargado exitosamente")

            // Imprimir información del modelo
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()
            println("Input shape: ${inputShape?.contentToString()}")
            println("Output shape: ${outputShape?.contentToString()}")

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Error al cargar el modelo: ${e.message}")
        }
    }

    private fun loadLabels() {
        try {
            labels = context.assets.open(LABELS_PATH).bufferedReader().readLines()
            println("✓ Labels cargadas: ${labels.size} clases")
            println("Clases: ${labels.joinToString(", ")}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("⚠ No se pudieron cargar las labels")
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val interpreter = this.interpreter ?: throw IllegalStateException("Modelo no inicializado")

        // Pre-procesamiento
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = preprocessImage(resizedBitmap)

        // Preparar output
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputShape[1] * outputShape[2])
        outputBuffer.order(ByteOrder.nativeOrder())

        // Inferencia
        val startTime = System.currentTimeMillis()
        interpreter.run(inputBuffer, outputBuffer)
        val inferenceTime = System.currentTimeMillis() - startTime
        println("⏱ Tiempo de inferencia: ${inferenceTime}ms")

        // Post-procesamiento
        outputBuffer.rewind()
        val output = FloatArray(outputShape[1] * outputShape[2])
        outputBuffer.asFloatBuffer().get(output)

        val detections = postprocess(output, outputShape, bitmap.width, bitmap.height)
        println("✓ Detecciones encontradas: ${detections.size}")

        return detections
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    private fun postprocess(
        output: FloatArray,
        outputShape: IntArray,
        originalWidth: Int,
        originalHeight: Int
    ): List<Detection> {
        val detections = mutableListOf<Detection>()

        // YOLO11 formato: [1, 84, 8400] o [1, num_classes + 4, num_predictions]
        val numFeatures = outputShape[1] // 84 (4 + 80 clases)
        val numPredictions = outputShape[2] // 8400
        val numClasses = numFeatures - 4

        println("Procesando: $numPredictions predicciones con $numClasses clases")

        for (i in 0 until numPredictions) {
            // Leer coordenadas
            val x = output[i]
            val y = output[numPredictions + i]
            val w = output[2 * numPredictions + i]
            val h = output[3 * numPredictions + i]

            // Encontrar clase con mayor confianza
            var maxConf = 0f
            var classId = -1

            for (c in 0 until numClasses) {
                val conf = output[(4 + c) * numPredictions + i]
                if (conf > maxConf) {
                    maxConf = conf
                    classId = c
                }
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                // Convertir coordenadas
                val scaleX = originalWidth.toFloat() / INPUT_SIZE
                val scaleY = originalHeight.toFloat() / INPUT_SIZE

                val left = ((x - w / 2) * scaleX).coerceIn(0f, originalWidth.toFloat())
                val top = ((y - h / 2) * scaleY).coerceIn(0f, originalHeight.toFloat())
                val right = ((x + w / 2) * scaleX).coerceIn(0f, originalWidth.toFloat())
                val bottom = ((y + h / 2) * scaleY).coerceIn(0f, originalHeight.toFloat())

                val boundingBox = RectF(left, top, right, bottom)
                val label = if (classId < labels.size) labels[classId] else "Unknown"

                detections.add(Detection(label, maxConf, boundingBox))
            }
        }

        return nms(detections)
    }

    private fun nms(detections: List<Detection>): List<Detection> {
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<Detection>()

        for (detection in sortedDetections) {
            var shouldSelect = true

            for (selected in selectedDetections) {
                if (detection.label == selected.label) {
                    val iou = calculateIoU(detection.boundingBox, selected.boundingBox)
                    if (iou > IOU_THRESHOLD) {
                        shouldSelect = false
                        break
                    }
                }
            }

            if (shouldSelect) {
                selectedDetections.add(detection)
            }
        }

        return selectedDetections
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)

        val intersectionArea = max(0f, intersectionRight - intersectionLeft) *
                max(0f, intersectionBottom - intersectionTop)

        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)

        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}