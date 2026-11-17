package com.example.scanner.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.scanner.ScannerViewModel
import com.example.scanner.ml.CategoryMapper
import com.example.scanner.model.DetectedItemUi
import com.example.scanner.model.ScanMode
import com.example.scanner.model.ScannerUiState
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassDark
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
  uiState: ScannerUiState,
  viewModel: ScannerViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

  val cameraController = remember(context) {
    LifecycleCameraController(context).apply {
      cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
      setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
      isTapToFocusEnabled = true
      isPinchToZoomEnabled = true
    }
  }

  DisposableEffect(lifecycleOwner, cameraController) {
    runCatching {
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.initializationFuture.addListener(
          {
            runCatching {
                val hasFlash = cameraController.cameraInfo?.hasFlashUnit() == true
                viewModel.onFlashAvailabilityChanged(hasFlash)
                viewModel.onCameraError(null)
              }
              .onFailure { error -> viewModel.onCameraError(error.localizedMessage) }
          },
          mainExecutor,
        )
      }
      .onFailure { error -> viewModel.onCameraError(error.localizedMessage) }

    onDispose {
      cameraController.clearImageAnalysisAnalyzer()
      cameraController.unbind()
      analysisExecutor.shutdown()
    }
  }

  val mlKitAnalyzer =
    remember(viewModel.liveDetector, mainExecutor) {
      MlKitAnalyzer(
        listOf(viewModel.liveDetector),
        CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
        mainExecutor,
      ) { result ->
        val detected = result?.getValue(viewModel.liveDetector) ?: emptyList()
        viewModel.onLiveObjectsDetected(detected)
      }
    }

  LaunchedEffect(uiState.scanMode, uiState.isShowingCapturedPhoto, cameraController) {
    if (uiState.scanMode == ScanMode.LIVE && !uiState.isShowingCapturedPhoto) {
      cameraController.setImageAnalysisAnalyzer(analysisExecutor, mlKitAnalyzer)
    } else {
      cameraController.clearImageAnalysisAnalyzer()
    }
  }

  LaunchedEffect(uiState.isTorchEnabled, uiState.hasFlashUnit) {
    if (uiState.hasFlashUnit) {
      runCatching { cameraController.enableTorch(uiState.isTorchEnabled) }
    }
  }

  var topHudHeightPx by remember { mutableIntStateOf(0) }
  var bottomDockHeightPx by remember { mutableIntStateOf(0) }
  val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
  val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

  val triggerCapture: () -> Unit = {
    if (!uiState.isAnalyzingPhoto) {
      viewModel.onPhotoCaptureStarted()
      cameraController.takePicture(
        analysisExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
          override fun onCaptureSuccess(image: ImageProxy) {
            val rotationDegrees = image.imageInfo.rotationDegrees
            val bitmapResult = runCatching {
              val rawBitmap = image.toBitmap()
              if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true,
                  )
                  .also { rotated -> if (rotated !== rawBitmap) rawBitmap.recycle() }
              } else {
                rawBitmap
              }
            }
            image.close()

            coroutineScope.launch(Dispatchers.Main) {
              bitmapResult
                .onSuccess { bitmap -> viewModel.onAnalyzeCapturedBitmap(bitmap) }
                .onFailure { err -> viewModel.onPhotoCaptureFailed(err.localizedMessage) }
            }
          }

          override fun onError(exception: ImageCaptureException) {
            coroutineScope.launch(Dispatchers.Main) {
              viewModel.onPhotoCaptureFailed(exception.localizedMessage)
            }
          }
        },
      )
    }
  }

  Box(modifier = modifier.fillMaxSize().background(ObsidianBg)) {
    // 1. Live CameraX Preview
    AndroidView(
      factory = { ctx ->
        PreviewView(ctx).apply {
          scaleType = PreviewView.ScaleType.FILL_CENTER
          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          controller = cameraController
        }
      },
      update = { previewView ->
        if (previewView.controller !== cameraController) {
          previewView.controller = cameraController
        }
      },
      modifier = Modifier.fillMaxSize().testTag("camera_preview"),
    )

    // 2. Captured Photo Layer (when frozen in Live mode or captured in Photo mode)
    val capturedPhoto = uiState.capturedPhoto
    if (capturedPhoto != null) {
      Image(
        bitmap = capturedPhoto.bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().testTag("captured_photo_image"),
      )
    }

    // 3. Subtle top & bottom gradient scrims for crisp readability
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(140.dp)
          .align(Alignment.TopCenter)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0xAA04070D), Color.Transparent),
            )
          )
    )
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(240.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, Color(0xCC04070D)),
            )
          )
    )

    // 4. Visual Bounding Boxes & Floating Ukrainian Label Pills
    DetectionOverlay(
      objects = uiState.activeObjects,
      selectedObjectId = uiState.selectedObjectId,
      showIdleReticle = !uiState.isShowingCapturedPhoto && uiState.cameraErrorMessage == null,
      topSafePx = maxOf(statusBarTopPx, topHudHeightPx.toFloat()),
      bottomSafePx = maxOf(navBarBottomPx, bottomDockHeightPx.toFloat()),
      onObjectClick = viewModel::onSelectObject,
      modifier = Modifier.fillMaxSize(),
    )

    // 5. Camera Error Card (if camera unavailable)
    if (uiState.cameraErrorMessage != null) {
      CameraErrorCard(
        onRetry = {
          runCatching {
            cameraController.unbind()
            cameraController.bindToLifecycle(lifecycleOwner)
            viewModel.onCameraError(null)
          }
        },
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
      )
    }

    // 6. Top Floating HUD
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .onSizeChanged { topHudHeightPx = it.height },
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Brand + Offline Pill
        Surface(
          shape = RoundedCornerShape(50),
          color = GlassDark,
          border = BorderStroke(1.dp, GlassBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.size(9.dp)
                  .background(
                    color = if (uiState.isAnalyzingPhoto) AmberWarm else ElectricCyan,
                    shape = CircleShape,
                  )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(50),
              color = ElectricCyan.copy(alpha = 0.14f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.WifiOff,
                  contentDescription = null,
                  tint = ElectricCyan,
                  modifier = Modifier.size(11.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.offline_badge),
                  style = MaterialTheme.typography.labelSmall,
                  color = ElectricCyan,
                )
              }
            }
          }
        }

        // Action Buttons (Torch + Help)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (uiState.hasFlashUnit && !uiState.isShowingCapturedPhoto) {
            HudCircleButton(
              icon = if (uiState.isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
              contentDescription =
                stringResource(
                  if (uiState.isTorchEnabled) R.string.action_torch_off
                  else R.string.action_torch_on
                ),
              isActive = uiState.isTorchEnabled,
              activeColor = AmberWarm,
              onClick = viewModel::onToggleTorch,
              testTag = "flashlight_button",
            )
          }

          HudCircleButton(
            icon = Icons.AutoMirrored.Rounded.HelpOutline,
            contentDescription = stringResource(R.string.action_help),
            isActive = false,
            activeColor = ElectricCyan,
            onClick = { viewModel.onShowHelpSheet(true) },
            testTag = "help_button",
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Contextual Status Hint Pill
      val statusText =
        when {
          uiState.isAnalyzingPhoto -> stringResource(R.string.status_analyzing)
          uiState.isShowingCapturedPhoto ->
            CategoryMapper.buildStatusSummary(context, uiState.activeObjects)
          uiState.scanMode == ScanMode.LIVE && uiState.liveObjects.isNotEmpty() ->
            CategoryMapper.buildStatusSummary(context, uiState.liveObjects)
          uiState.scanMode == ScanMode.LIVE -> stringResource(R.string.hint_point_camera)
          else -> stringResource(R.string.hint_press_scan)
        }

      Surface(
        shape = RoundedCornerShape(50),
        color = GlassDark,
        border = BorderStroke(1.dp, GlassBorder),
      ) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = TextPrimary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
      }
    }

    // 7. Bottom Control Deck
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
          .onSizeChanged { bottomDockHeightPx = it.height },
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Quick-select chip strip when objects are detected
      AnimatedVisibility(
        visible = uiState.activeObjects.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        DetectedObjectsChipStrip(
          objects = uiState.activeObjects,
          selectedObjectId = uiState.selectedObjectId,
          onSelectObject = viewModel::onSelectObject,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }

      Column(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Mode Switcher ("Наживо" vs "Фото")
        if (!uiState.isShowingCapturedPhoto) {
          ModeTogglePill(
            currentMode = uiState.scanMode,
            onModeSelected = viewModel::onSetScanMode,
          )
          Spacer(modifier = Modifier.height(18.dp))
        }

        // Primary Shutter / Scan / Rescan Action
        if (uiState.isShowingCapturedPhoto) {
          Button(
            onClick = viewModel::onRetakePhoto,
            enabled = !uiState.isAnalyzingPhoto,
            modifier =
              Modifier.height(56.dp)
                .widthIn(min = 220.dp)
                .testTag("rescan_button"),
            shape = RoundedCornerShape(50),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = ObsidianBg,
              ),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.action_rescan),
              style = MaterialTheme.typography.titleMedium,
            )
          }
        } else {
          ShutterScanButton(
            scanMode = uiState.scanMode,
            isAnalyzing = uiState.isAnalyzingPhoto,
            onClick = triggerCapture,
          )
        }
      }
    }

    // 8. Ukrainian Help Bottom Sheet
    if (uiState.isHelpSheetVisible) {
      ModalBottomSheet(
        onDismissRequest = { viewModel.onShowHelpSheet(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
      ) {
        HelpSheetContent(onClose = { viewModel.onShowHelpSheet(false) })
      }
    }
  }
}

@Composable
private fun HudCircleButton(
  icon: ImageVector,
  contentDescription: String,
  isActive: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val bgColor by
    animateColorAsState(
      targetValue = if (isActive) activeColor.copy(alpha = 0.22f) else GlassDark,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "hud_btn_bg",
    )
  val borderColor = if (isActive) activeColor.copy(alpha = 0.85f) else GlassBorder

  Surface(
    shape = CircleShape,
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.size(48.dp),
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier.fillMaxSize().minimumInteractiveComponentSize().testTag(testTag),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (isActive) activeColor else TextPrimary,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

@Composable
private fun ModeTogglePill(
  currentMode: ScanMode,
  onModeSelected: (ScanMode) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = GlassDark,
    border = BorderStroke(1.dp, GlassBorder),
  ) {
    Row(
      modifier = Modifier.padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ModeTabItem(
        label = stringResource(R.string.mode_live),
        icon = Icons.Rounded.AutoAwesome,
        selected = currentMode == ScanMode.LIVE,
        onClick = { onModeSelected(ScanMode.LIVE) },
        testTag = "mode_live_button",
      )
      ModeTabItem(
        label = stringResource(R.string.mode_photo),
        icon = Icons.Rounded.PhotoCamera,
        selected = currentMode == ScanMode.PHOTO,
        onClick = { onModeSelected(ScanMode.PHOTO) },
        testTag = "mode_photo_button",
      )
    }
  }
}

@Composable
private fun ModeTabItem(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val containerColor by
    animateColorAsState(
      targetValue = if (selected) ElectricCyan else Color.Transparent,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "mode_tab_bg",
    )
  val contentColor by
    animateColorAsState(
      targetValue = if (selected) ObsidianBg else TextPrimary,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "mode_tab_fg",
    )

  Surface(
    shape = RoundedCornerShape(50),
    color = containerColor,
    modifier =
      Modifier.height(44.dp)
        .clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .semantics(mergeDescendants = true) {}
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(17.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
      )
    }
  }
}

@Composable
private fun ShutterScanButton(
  scanMode: ScanMode,
  isAnalyzing: Boolean,
  onClick: () -> Unit,
) {
  val actionLabel =
    stringResource(
      if (scanMode == ScanMode.LIVE) R.string.action_freeze else R.string.action_scan
    )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier =
        Modifier.size(84.dp)
          .clip(CircleShape)
          .border(width = 3.dp, color = ElectricCyan.copy(alpha = 0.85f), shape = CircleShape)
          .padding(6.dp)
          .clip(CircleShape)
          .background(
            brush =
              Brush.linearGradient(
                colors =
                  if (scanMode == ScanMode.LIVE) listOf(ElectricCyan, Color(0xFF06B6D4))
                  else listOf(Color.White, Color(0xFFE2E8F0))
              )
          )
          .clickable(enabled = !isAnalyzing, onClick = onClick)
          .semantics(mergeDescendants = true) {}
          .testTag("shutter_button"),
      contentAlignment = Alignment.Center,
    ) {
      if (isAnalyzing) {
        CircularProgressIndicator(
          modifier = Modifier.size(28.dp),
          color = ObsidianBg,
          strokeWidth = 3.dp,
        )
      } else {
        Icon(
          imageVector =
            if (scanMode == ScanMode.LIVE) Icons.Rounded.CenterFocusStrong
            else Icons.Rounded.CameraAlt,
          contentDescription = actionLabel,
          tint = ObsidianBg,
          modifier = Modifier.size(30.dp),
        )
      }
    }

    Text(
      text = actionLabel,
      style = MaterialTheme.typography.labelMedium,
      color = TextPrimary,
    )
  }
}

@Composable
private fun DetectedObjectsChipStrip(
  objects: List<DetectedItemUi>,
  selectedObjectId: String?,
  onSelectObject: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    contentPadding = PaddingValues(horizontal = 8.dp),
  ) {
    items(objects, key = { it.id }) { item ->
      val isSelected = selectedObjectId == item.id
      val accent = item.category.accentColor

      Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) accent.copy(alpha = 0.24f) else GlassDark,
        border = BorderStroke(1.dp, accent.copy(alpha = if (isSelected) 0.95f else 0.45f)),
        modifier =
          Modifier.height(40.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onSelectObject(item.id) },
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = item.category.icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = item.labelUk,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
          )
          if (item.confidencePercent != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${item.confidencePercent}%",
              style = MaterialTheme.typography.labelSmall,
              color = accent,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CameraErrorCard(
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.widthIn(max = 360.dp),
    shape = RoundedCornerShape(24.dp),
    color = ObsidianSurface,
    border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.camera_error_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.camera_error_body),
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onRetry,
        modifier = Modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
      ) {
        Text(text = stringResource(R.string.camera_error_retry))
      }
    }
  }
}

@Composable
private fun HelpSheetContent(onClose: () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(R.string.help_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
      )
      IconButton(
        onClick = onClose,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = stringResource(R.string.action_close),
          tint = TextSecondary,
        )
      }
    }

    Text(
      text = stringResource(R.string.help_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary,
    )

    HelpInfoRow(
      icon = Icons.Rounded.AutoAwesome,
      title = stringResource(R.string.help_mode_live_title),
      body = stringResource(R.string.help_mode_live_desc),
    )
    HelpInfoRow(
      icon = Icons.Rounded.PhotoCamera,
      title = stringResource(R.string.help_mode_photo_title),
      body = stringResource(R.string.help_mode_photo_desc),
    )
    HelpInfoRow(
      icon = Icons.Rounded.CenterFocusStrong,
      title = stringResource(R.string.help_categories_title),
      body = stringResource(R.string.help_categories_desc),
    )
  }
}

@Composable
private fun HelpInfoRow(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = ObsidianSurfaceElevated,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = ElectricCyan,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Text(
          text = body,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
    }
  }
}
