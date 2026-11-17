package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scanner.ScannerViewModel
import com.example.scanner.haptics.HapticFeedbackManager
import com.example.scanner.ui.PermissionScreen
import com.example.scanner.ui.ScannerScreen
import com.example.ui.theme.LensScannerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    setContent {
      LensScannerTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val scannerViewModel: ScannerViewModel = viewModel()
        val uiState by scannerViewModel.uiState.collectAsStateWithLifecycle()
        val hapticManager = remember(context) { HapticFeedbackManager(context) }

        LaunchedEffect(scannerViewModel) {
          scannerViewModel.lockOnEvents.collect {
            hapticManager.pulseLockOn()
          }
        }

        var hasCameraPermission by remember {
          mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
              PackageManager.PERMISSION_GRANTED
          )
        }
        var hasAskedPermission by rememberSaveable { mutableStateOf(false) }

        val permissionLauncher =
          rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { granted ->
            hasCameraPermission = granted
            hasAskedPermission = true
          }

        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              hasCameraPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                  PackageManager.PERMISSION_GRANTED
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (hasCameraPermission) {
          ScannerScreen(
            uiState = uiState,
            viewModel = scannerViewModel,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
              this@MainActivity,
              Manifest.permission.CAMERA,
            )
          val isPermanentlyDenied = hasAskedPermission && !shouldShowRationale

          PermissionScreen(
            isPermanentlyDenied = isPermanentlyDenied,
            onRequestPermission = {
              permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onOpenSettings = {
              val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                  data = Uri.fromParts("package", packageName, null)
                }
              startActivity(intent)
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}
