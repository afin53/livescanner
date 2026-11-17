package com.example.scanner.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.scanner.model.DetectedItemUi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ObjectDetectionEngine(private val context: Context) {

  val liveDetector: ObjectDetector by lazy {
    val options =
      ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    ObjectDetection.getClient(options)
  }

  private val singleImageDetector: ObjectDetector by lazy {
    val options =
      ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    ObjectDetection.getClient(options)
  }

  fun mapLiveResults(detectedObjects: List<DetectedObject>): List<DetectedItemUi> {
    return detectedObjects.mapIndexed { index, obj ->
      CategoryMapper.toDetectedItemUi(
        detectedObject = obj,
        index = index,
        context = context,
        sourceWidth = 0,
        sourceHeight = 0,
        isViewReferenced = true,
      )
    }
  }

  suspend fun detectInBitmap(bitmap: Bitmap): List<DetectedItemUi> =
    suspendCancellableCoroutine { continuation ->
      val inputImage = InputImage.fromBitmap(bitmap, 0)
      singleImageDetector
        .process(inputImage)
        .addOnSuccessListener { results ->
          if (continuation.isActive) {
            val mapped =
              results.mapIndexed { index, obj ->
                CategoryMapper.toDetectedItemUi(
                  detectedObject = obj,
                  index = index,
                  context = context,
                  sourceWidth = bitmap.width,
                  sourceHeight = bitmap.height,
                  isViewReferenced = false,
                )
              }
            continuation.resume(mapped)
          }
        }
        .addOnFailureListener { error ->
          if (continuation.isActive) {
            continuation.resumeWithException(error)
          }
        }
    }

  fun close() {
    runCatching { liveDetector.close() }
    runCatching { singleImageDetector.close() }
  }
}
