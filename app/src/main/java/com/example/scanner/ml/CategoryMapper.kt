package com.example.scanner.ml

import android.content.Context
import android.graphics.RectF
import com.example.R
import com.example.scanner.model.DetectedItemUi
import com.example.scanner.model.ObjectCategory
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.defaults.PredefinedCategory
import kotlin.math.roundToInt

object CategoryMapper {

  fun mapCategory(label: DetectedObject.Label?): ObjectCategory {
    if (label == null) return ObjectCategory.UNKNOWN
    return when {
      label.index == PredefinedCategory.FASHION_GOOD_INDEX ||
        label.text.equals(PredefinedCategory.FASHION_GOOD, ignoreCase = true) ->
        ObjectCategory.FASHION_GOOD
      label.index == PredefinedCategory.FOOD_INDEX ||
        label.text.equals(PredefinedCategory.FOOD, ignoreCase = true) ->
        ObjectCategory.FOOD
      label.index == PredefinedCategory.HOME_GOOD_INDEX ||
        label.text.equals(PredefinedCategory.HOME_GOOD, ignoreCase = true) ->
        ObjectCategory.HOME_GOOD
      label.index == PredefinedCategory.PLACE_INDEX ||
        label.text.equals(PredefinedCategory.PLACE, ignoreCase = true) ->
        ObjectCategory.PLACE
      label.index == PredefinedCategory.PLANT_INDEX ||
        label.text.equals(PredefinedCategory.PLANT, ignoreCase = true) ->
        ObjectCategory.PLANT
      else -> ObjectCategory.UNKNOWN
    }
  }

  fun toDetectedItemUi(
    detectedObject: DetectedObject,
    index: Int,
    context: Context,
    sourceWidth: Int,
    sourceHeight: Int,
    isViewReferenced: Boolean,
  ): DetectedItemUi {
    val bestLabel = detectedObject.labels.maxByOrNull { it.confidence }
    val category = mapCategory(bestLabel)
    val labelText = context.getString(category.labelResId)
    val confidencePercent = bestLabel?.let {
      (it.confidence * 100f).roundToInt().coerceIn(1, 100)
    }
    val stableId = detectedObject.trackingId?.let { "track_$it" } ?: "obj_${index}_${category.name}"

    return DetectedItemUi(
      id = stableId,
      trackingId = detectedObject.trackingId,
      category = category,
      labelUk = labelText,
      confidencePercent = confidencePercent,
      boundingBox = RectF(detectedObject.boundingBox),
      sourceWidth = sourceWidth,
      sourceHeight = sourceHeight,
      isViewReferenced = isViewReferenced,
    )
  }

  fun buildStatusSummary(context: Context, objects: List<DetectedItemUi>): String {
    return when (objects.size) {
      0 -> context.getString(R.string.status_nothing_found)
      1 -> {
        val item = objects.first()
        val labelWithConfidence =
          if (item.confidencePercent != null) "${item.labelUk} (${item.confidencePercent}%)"
          else item.labelUk
        context.getString(R.string.status_found_one, labelWithConfidence)
      }
      else -> context.getString(R.string.status_found_many, objects.size)
    }
  }
}
