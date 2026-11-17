package com.example.scanner.model

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.VioletGlow

enum class ScanMode {
  LIVE,
  PHOTO,
}

enum class ObjectCategory(
  val accentColor: Color,
  val icon: ImageVector,
  val labelResId: Int,
) {
  FASHION_GOOD(
    accentColor = VioletGlow,
    icon = Icons.Rounded.Checkroom,
    labelResId = R.string.category_fashion,
  ),
  FOOD(
    accentColor = AmberWarm,
    icon = Icons.Rounded.Restaurant,
    labelResId = R.string.category_food,
  ),
  HOME_GOOD(
    accentColor = ElectricCyan,
    icon = Icons.Rounded.Chair,
    labelResId = R.string.category_home,
  ),
  PLACE(
    accentColor = SkyBlue,
    icon = Icons.Rounded.LocationOn,
    labelResId = R.string.category_place,
  ),
  PLANT(
    accentColor = EmeraldMint,
    icon = Icons.Rounded.Spa,
    labelResId = R.string.category_plant,
  ),
  UNKNOWN(
    accentColor = ElectricCyan,
    icon = Icons.Rounded.ViewInAr,
    labelResId = R.string.category_unknown,
  ),
}

data class DetectedItemUi(
  val id: String,
  val trackingId: Int?,
  val category: ObjectCategory,
  val labelUk: String,
  val confidencePercent: Int?,
  val boundingBox: RectF,
  val sourceWidth: Int,
  val sourceHeight: Int,
  val isViewReferenced: Boolean,
)

data class CapturedPhotoResult(
  val bitmap: Bitmap,
  val objects: List<DetectedItemUi>,
)

data class ScannerUiState(
  val scanMode: ScanMode = ScanMode.LIVE,
  val liveObjects: List<DetectedItemUi> = emptyList(),
  val capturedPhoto: CapturedPhotoResult? = null,
  val isAnalyzingPhoto: Boolean = false,
  val isTorchEnabled: Boolean = false,
  val hasFlashUnit: Boolean = false,
  val selectedObjectId: String? = null,
  val cameraErrorMessage: String? = null,
  val isHelpSheetVisible: Boolean = false,
) {
  val activeObjects: List<DetectedItemUi>
    get() = capturedPhoto?.objects ?: liveObjects

  val isShowingCapturedPhoto: Boolean
    get() = capturedPhoto != null
}
