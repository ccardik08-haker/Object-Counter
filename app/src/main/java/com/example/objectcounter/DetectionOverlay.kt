package com.example.objectcounter

import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import org.tensorflow.lite.task.vision.detector.Detection

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    imageProxy: ImageProxy?,
    previewView: PreviewView?,
    modifier: Modifier = Modifier
) {
    if (imageProxy == null || previewView == null || detections.isEmpty()) return

    Canvas(modifier = modifier) {
        val matrix = getCorrectionMatrix(imageProxy, previewView)

        detections.forEach { detection ->
            val box = detection.boundingBox
            val points = floatArrayOf(box.left, box.top, box.right, box.bottom)
            matrix.mapPoints(points)

            val left = points[0]
            val top = points[1]
            val right = points[2]
            val bottom = points[3]

            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 5f)
            )
        }
    }
}

fun getCorrectionMatrix(imageProxy: ImageProxy, previewView: PreviewView): Matrix {
    val cropRect = imageProxy.cropRect
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val matrix = Matrix()

    val source = floatArrayOf(
        cropRect.left.toFloat(), cropRect.top.toFloat(),
        cropRect.right.toFloat(), cropRect.top.toFloat(),
        cropRect.right.toFloat(), cropRect.bottom.toFloat(),
        cropRect.left.toFloat(), cropRect.bottom.toFloat()
    )

    val destination = floatArrayOf(
        0f, 0f,
        previewView.width.toFloat(), 0f,
        previewView.width.toFloat(), previewView.height.toFloat(),
        0f, previewView.height.toFloat()
    )

    val shiftOffset = rotationDegrees / 90 * 2
    val tempArray = destination.clone()
    for (toIndex in source.indices) {
        val fromIndex = (toIndex + shiftOffset) % source.size
        destination[toIndex] = tempArray[fromIndex]
    }
    matrix.setPolyToPoly(source, 0, destination, 0, 4)
    return matrix
}