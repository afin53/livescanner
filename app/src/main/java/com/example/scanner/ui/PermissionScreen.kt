package com.example.scanner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionScreen(
  isPermanentlyDenied: Boolean,
  onRequestPermission: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF0E2239), ObsidianBg),
            radius = 950f,
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(50),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.permission_privacy_pill),
            style = MaterialTheme.typography.labelMedium,
            color = ElectricCyan,
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Box(
        modifier =
          Modifier.size(116.dp)
            .background(
              brush =
                Brush.radialGradient(
                  colors = listOf(ElectricCyan.copy(alpha = 0.28f), Color.Transparent)
                ),
              shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier.size(84.dp),
          shape = CircleShape,
          color = ObsidianSurface,
          border = BorderStroke(2.dp, ElectricCyan.copy(alpha = 0.7f)),
          shadowElevation = 12.dp,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.CameraAlt,
              contentDescription = null,
              tint = ElectricCyan,
              modifier = Modifier.size(38.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = stringResource(R.string.permission_title),
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stringResource(R.string.permission_body),
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )

      if (isPermanentlyDenied) {
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = AmberWarm.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AmberWarm.copy(alpha = 0.4f)),
        ) {
          Text(
            text = stringResource(R.string.permission_denied_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AmberWarm,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = { if (isPermanentlyDenied) onOpenSettings() else onRequestPermission() },
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .testTag("grant_permission_button"),
        shape = RoundedCornerShape(20.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = ElectricCyan,
            contentColor = ObsidianBg,
          ),
      ) {
        if (isPermanentlyDenied) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text =
            stringResource(
              if (isPermanentlyDenied) R.string.permission_settings_button
              else R.string.permission_grant_button
            ),
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }
}
