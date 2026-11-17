package com.example.scanner

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanner.ml.ObjectDetectionEngine
import com.example.scanner.model.CapturedPhotoResult
import com.example.scanner.model.ScanMode
import com.example.scanner.model.ScannerUiState
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

  private val detectionEngine = ObjectDetectionEngine(application.applicationContext)

  val liveDetector: ObjectDetector
    get() = detectionEngine.liveDetector

  private val _uiState = MutableStateFlow(ScannerUiState())
  val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

  private val _lockOnEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val lockOnEvents: SharedFlow<Unit> = _lockOnEvents.asSharedFlow()

  private var knownTrackingIds: Set<Int> = emptySet()
  private var hadLiveObjectsPreviously: Boolean = false

  fun onLiveObjectsDetected(detectedObjects: List<DetectedObject>) {
    val currentState = _uiState.value
    if (currentState.scanMode != ScanMode.LIVE || currentState.isShowingCapturedPhoto) {
      return
    }

    val mapped = detectionEngine.mapLiveResults(detectedObjects)
    val currentTrackingIds = mapped.mapNotNull { it.trackingId }.toSet()

    val hasNewTrackedObject =
      currentTrackingIds.any { it !in knownTrackingIds } ||
        (!hadLiveObjectsPreviously && mapped.isNotEmpty())

    if (mapped.isEmpty()) {
      knownTrackingIds = emptySet()
      hadLiveObjectsPreviously = false
    } else {
      knownTrackingIds = currentTrackingIds
      hadLiveObjectsPreviously = true
      if (hasNewTrackedObject) {
        _lockOnEvents.tryEmit(Unit)
      }
    }

    _uiState.update { state ->
      val validSelection =
        state.selectedObjectId?.takeIf { selected -> mapped.any { it.id == selected } }
      state.copy(liveObjects = mapped, selectedObjectId = validSelection)
    }
  }

  fun onPhotoCaptureStarted() {
    _uiState.update { it.copy(isAnalyzingPhoto = true, selectedObjectId = null) }
  }

  fun onPhotoCaptureFailed(errorMessage: String?) {
    _uiState.update {
      it.copy(isAnalyzingPhoto = false, cameraErrorMessage = errorMessage)
    }
  }

  fun onAnalyzeCapturedBitmap(bitmap: Bitmap) {
    _uiState.update {
      it.copy(
        capturedPhoto = CapturedPhotoResult(bitmap = bitmap, objects = emptyList()),
        isAnalyzingPhoto = true,
        selectedObjectId = null,
      )
    }

    viewModelScope.launch {
      runCatching { detectionEngine.detectInBitmap(bitmap) }
        .onSuccess { detected ->
          if (detected.isNotEmpty()) {
            _lockOnEvents.tryEmit(Unit)
          }
          _uiState.update { state ->
            state.copy(
              capturedPhoto = CapturedPhotoResult(bitmap = bitmap, objects = detected),
              isAnalyzingPhoto = false,
              selectedObjectId = detected.firstOrNull()?.id,
            )
          }
        }
        .onFailure {
          _uiState.update { state ->
            state.copy(
              capturedPhoto = CapturedPhotoResult(bitmap = bitmap, objects = emptyList()),
              isAnalyzingPhoto = false,
            )
          }
        }
    }
  }

  fun onSetScanMode(mode: ScanMode) {
    if (_uiState.value.scanMode == mode && !_uiState.value.isShowingCapturedPhoto) return
    knownTrackingIds = emptySet()
    hadLiveObjectsPreviously = false
    _uiState.update {
      it.copy(
        scanMode = mode,
        liveObjects = emptyList(),
        capturedPhoto = null,
        isAnalyzingPhoto = false,
        selectedObjectId = null,
      )
    }
  }

  fun onRetakePhoto() {
    knownTrackingIds = emptySet()
    hadLiveObjectsPreviously = false
    _uiState.update {
      it.copy(
        capturedPhoto = null,
        liveObjects = emptyList(),
        isAnalyzingPhoto = false,
        selectedObjectId = null,
      )
    }
  }

  fun onToggleTorch() {
    _uiState.update { state ->
      if (!state.hasFlashUnit) state else state.copy(isTorchEnabled = !state.isTorchEnabled)
    }
  }

  fun onFlashAvailabilityChanged(hasFlash: Boolean) {
    _uiState.update {
      it.copy(
        hasFlashUnit = hasFlash,
        isTorchEnabled = if (hasFlash) it.isTorchEnabled else false,
      )
    }
  }

  fun onSelectObject(objectId: String?) {
    _uiState.update { state ->
      val nextId = if (state.selectedObjectId == objectId) null else objectId
      state.copy(selectedObjectId = nextId)
    }
  }

  fun onCameraError(message: String?) {
    _uiState.update { it.copy(cameraErrorMessage = message) }
  }

  fun onShowHelpSheet(visible: Boolean) {
    _uiState.update { it.copy(isHelpSheetVisible = visible) }
  }

  override fun onCleared() {
    super.onCleared()
    detectionEngine.close()
  }
}
