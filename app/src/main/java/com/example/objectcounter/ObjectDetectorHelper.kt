package com.example.objectcounter

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(context: Context) {
    private var detector: ObjectDetector? = null

    init {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(20)
            .setScoreThreshold(0.4f)
            .build()
        try {
            detector = ObjectDetector.createFromFileAndOptions(context, "model.tflite", options)
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Ошибка загрузки: ${e.message}")
        }
    }

    fun detect(imageProxy: ImageProxy): List<Detection> {
        val bitmap = imageProxy.toBitmap()
        val image = TensorImage.fromBitmap(bitmap)
        val results = detector?.detect(image) ?: emptyList()
        imageProxy.close()
        return results
    }
}