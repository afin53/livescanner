package com.example

import android.content.Context
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.scanner.ml.CategoryMapper
import com.example.scanner.model.DetectedItemUi
import com.example.scanner.model.ObjectCategory
import com.example.scanner.ui.mapBoundingBoxToCanvas
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.defaults.PredefinedCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Лінза ШІ", appName)
  }

  @Test
  fun `category mapper translates predefined categories to Ukrainian`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(
      "Рослина",
      context.getString(
        CategoryMapper.mapCategory(
            DetectedObject.Label(PredefinedCategory.PLANT, 0.9f, PredefinedCategory.PLANT_INDEX)
          )
          .labelResId
      ),
    )
    assertEquals(
      "Їжа та напої",
      context.getString(
        CategoryMapper.mapCategory(
            DetectedObject.Label(PredefinedCategory.FOOD, 0.85f, PredefinedCategory.FOOD_INDEX)
          )
          .labelResId
      ),
    )
    assertEquals(
      "Одяг та аксесуари",
      context.getString(
        CategoryMapper.mapCategory(
            DetectedObject.Label(
              PredefinedCategory.FASHION_GOOD,
              0.7f,
              PredefinedCategory.FASHION_GOOD_INDEX,
            )
          )
          .labelResId
      ),
    )
  }

  @Test
  fun `mapBoundingBoxToCanvas applies ContentScale Crop correctly`() {
    val item =
      DetectedItemUi(
        id = "test",
        trackingId = 1,
        category = ObjectCategory.HOME_GOOD,
        labelUk = "Товари для дому",
        confidencePercent = 91,
        boundingBox = RectF(100f, 100f, 300f, 300f),
        sourceWidth = 1000,
        sourceHeight = 1000,
        isViewReferenced = false,
      )
    // Canvas 1000x2000 -> scale = max(1, 2) = 2f, offsetX = (1000 - 2000)/2 = -500f, offsetY = 0f
    val mapped = mapBoundingBoxToCanvas(item, 1000f, 2000f)
    assertEquals(-300f, mapped.left, 0.01f)
    assertEquals(200f, mapped.top, 0.01f)
    assertEquals(100f, mapped.right, 0.01f)
    assertEquals(600f, mapped.bottom, 0.01f)
  }
}
