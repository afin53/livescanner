package com.example.scanner.ui

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.scanner.model.DetectedItemUi
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassDark
import com.example.ui.theme.TextPrimary
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DetectionOverlay(
  objects: List<DetectedItemUi>,
  selectedObjectId: String?,
  showIdleReticle: Boolean,
  topSafePx: Float,
  bottomSafePx: Float,
  onObjectClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "scanner_glow")
  val glowPulse by
    infiniteTransition.animateFloat(
      initialValue = 0.24f,
      targetValue = 0.46f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "glow_pulse",
    )

  val reticleBreath by
    infiniteTransition.animateFloat(
      initialValue = 0.94f,
      targetValue = 1.03f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "reticle_breath",
    )

  val density = LocalDensity.current
  val pillSizes = remember { mutableStateMapOf<String, IntSize>() }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val canvasWidthPx = with(density) { maxWidth.toPx() }
    val canvasHeightPx = with(density) { maxHeight.toPx() }

    val mappedItems =
      remember(objects, canvasWidthPx, canvasHeightPx) {
        objects.map { item ->
          item to mapBoundingBoxToCanvas(item, canvasWidthPx, canvasHeightPx)
        }
      }

    Canvas(modifier = Modifier.fillMaxSize()) {
      if (showIdleReticle && mappedItems.isEmpty()) {
        drawIdleViewfinderReticle(scaleFactor = reticleBreath)
      }

      mappedItems.forEach { (item, rect) ->
        val isSelected = selectedObjectId == null || selectedObjectId == item.id
        drawGlowingBoundingBox(
          rect = rect,
          accentColor = item.category.accentColor,
          glowAlpha = if (item.id == selectedObjectId) glowPulse + 0.16f else glowPulse,
          dimmed = !isSelected,
        )
      }
    }

    val edgeMarginPx = with(density) { 12.dp.toPx() }
    val pillGapPx = with(density) { 8.dp.toPx() }
    val safeTop = topSafePx + edgeMarginPx
    val safeBottom = (canvasHeightPx - bottomSafePx - edgeMarginPx).coerceAtLeast(safeTop + 60f)

    mappedItems.forEach { (item, rect) ->
      val isSelected = selectedObjectId == null || selectedObjectId == item.id
      val measuredSize = pillSizes[item.id] ?: IntSize.Zero

      val xPx =
        rect.left
          .coerceIn(
            edgeMarginPx,
            (canvasWidthPx - measuredSize.width - edgeMarginPx).coerceAtLeast(edgeMarginPx),
          )
          .roundToInt()

      val preferredAboveY = rect.top - measuredSize.height - pillGapPx
      val yPx =
        if (preferredAboveY >= safeTop) {
            preferredAboveY
          } else {
            (rect.top + pillGapPx).coerceAtMost(safeBottom - measuredSize.height)
          }
          .coerceIn(safeTop, (safeBottom - measuredSize.height).coerceAtLeast(safeTop))
          .roundToInt()

      FloatingLabelPill(
        item = item,
        isHighlighted = item.id == selectedObjectId,
        isDimmed = !isSelected,
        onClick = { onObjectClick(item.id) },
        modifier =
          Modifier.offset { IntOffset(x = xPx, y = yPx) }
            .onSizeChanged { size -> pillSizes[item.id] = size },
      )
    }
  }
}

@Composable
private fun FloatingLabelPill(
  item: DetectedItemUi,
  isHighlighted: Boolean,
  isDimmed: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = item.category.accentColor
  val alphaMultiplier = if (isDimmed) 0.52f else 1f
  val pillShape = RoundedCornerShape(50)

  Surface(
    modifier =
      modifier
        .clip(pillShape)
        .clickable(onClick = onClick)
        .semantics(mergeDescendants = true) {},
    shape = pillShape,
    color = GlassDark.copy(alpha = 0.88f * alphaMultiplier),
    border =
      BorderStroke(
        width = if (isHighlighted) 1.6.dp else 1.1.dp,
        color = accent.copy(alpha = (if (isHighlighted) 0.95f else 0.7f) * alphaMultiplier),
      ),
    shadowElevation = if (isHighlighted) 10.dp else 4.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = item.category.icon,
        contentDescription = null,
        tint = accent.copy(alpha = alphaMultiplier),
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = item.labelUk,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary.copy(alpha = alphaMultiplier),
      )
      Spacer(modifier = Modifier.width(6.dp))
      val badgeText =
        if (item.confidencePercent != null) "${item.confidencePercent}%"
        else stringResource(R.string.detected_badge_unclassified)
      Text(
        text = badgeText,
        style = MaterialTheme.typography.labelSmall,
        color = accent.copy(alpha = alphaMultiplier),
        modifier =
          Modifier.background(
              color = accent.copy(alpha = 0.16f * alphaMultiplier),
              shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
  }
}

fun mapBoundingBoxToCanvas(
  item: DetectedItemUi,
  canvasWidth: Float,
  canvasHeight: Float,
): RectF {
  if (item.isViewReferenced || item.sourceWidth <= 0 || item.sourceHeight <= 0) {
    return RectF(item.boundingBox)
  }

  val bmpW = item.sourceWidth.toFloat()
  val bmpH = item.sourceHeight.toFloat()
  val scale = maxOf(canvasWidth / bmpW, canvasHeight / bmpH)
  val drawnW = bmpW * scale
  val drawnH = bmpH * scale
  val offsetX = (canvasWidth - drawnW) / 2f
  val offsetY = (canvasHeight - drawnH) / 2f

  return RectF(
    item.boundingBox.left * scale + offsetX,
    item.boundingBox.top * scale + offsetY,
    item.boundingBox.right * scale + offsetX,
    item.boundingBox.bottom * scale + offsetY,
  )
}

private fun DrawScope.drawGlowingBoundingBox(
  rect: RectF,
  accentColor: Color,
  glowAlpha: Float,
  dimmed: Boolean,
) {
  val left = rect.left.coerceIn(0f, size.width)
  val top = rect.top.coerceIn(0f, size.height)
  val right = rect.right.coerceIn(left, size.width)
  val bottom = rect.bottom.coerceIn(top, size.height)
  val width = (right - left).coerceAtLeast(8f)
  val height = (bottom - top).coerceAtLeast(8f)

  val factor = if (dimmed) 0.42f else 1f
  val cornerRadiusPx = min(20.dp.toPx(), min(width, height) * 0.25f)
  val boxTopLeft = Offset(left, top)
  val boxSize = Size(width, height)
  val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)

  // Soft interior tint
  drawRoundRect(
    color = accentColor.copy(alpha = 0.08f * factor),
    topLeft = boxTopLeft,
    size = boxSize,
    cornerRadius = cornerRadius,
  )

  // Outer glowing aura
  drawRoundRect(
    color = accentColor.copy(alpha = glowAlpha * factor),
    topLeft = boxTopLeft,
    size = boxSize,
    cornerRadius = cornerRadius,
    style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  // Crisp subtle inner frame
  drawRoundRect(
    color = accentColor.copy(alpha = 0.48f * factor),
    topLeft = boxTopLeft,
    size = boxSize,
    cornerRadius = cornerRadius,
    style = Stroke(width = 1.2.dp.toPx()),
  )

  // Glowing rounded L-brackets on the four corners
  val bracketLen = min(26.dp.toPx(), min(width, height) * 0.28f)
  val bracketStroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
  val bracketColor = accentColor.copy(alpha = 0.96f * factor)

  // Top-left bracket
  drawPath(
    path =
      Path().apply {
        moveTo(left, top + bracketLen)
        lineTo(left, top + cornerRadiusPx)
        quadraticTo(left, top, left + cornerRadiusPx, top)
        lineTo(left + bracketLen, top)
      },
    color = bracketColor,
    style = bracketStroke,
  )

  // Top-right bracket
  drawPath(
    path =
      Path().apply {
        moveTo(right - bracketLen, top)
        lineTo(right - cornerRadiusPx, top)
        quadraticTo(right, top, right, top + cornerRadiusPx)
        lineTo(right, top + bracketLen)
      },
    color = bracketColor,
    style = bracketStroke,
  )

  // Bottom-right bracket
  drawPath(
    path =
      Path().apply {
        moveTo(right, bottom - bracketLen)
        lineTo(right, bottom - cornerRadiusPx)
        quadraticTo(right, bottom, right - cornerRadiusPx, bottom)
        lineTo(right - bracketLen, bottom)
      },
    color = bracketColor,
    style = bracketStroke,
  )

  // Bottom-left bracket
  drawPath(
    path =
      Path().apply {
        moveTo(left + bracketLen, bottom)
        lineTo(left + cornerRadiusPx, bottom)
        quadraticTo(left, bottom, left, bottom - cornerRadiusPx)
        lineTo(left, bottom - bracketLen)
      },
    color = bracketColor,
    style = bracketStroke,
  )

  // Center optical anchor dot
  val center = Offset(left + width / 2f, top + height / 2f)
  drawCircle(color = accentColor.copy(alpha = 0.28f * factor), radius = 8.dp.toPx(), center = center)
  drawCircle(color = Color.White.copy(alpha = 0.85f * factor), radius = 3.dp.toPx(), center = center)
}

private fun DrawScope.drawIdleViewfinderReticle(scaleFactor: Float) {
  val side = min(size.width, size.height) * 0.56f * scaleFactor
  val left = (size.width - side) / 2f
  val top = (size.height - side) / 2f - 24.dp.toPx()
  val right = left + side
  val bottom = top + side
  val radius = 24.dp.toPx()
  val len = 34.dp.toPx()

  val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
  val color = ElectricCyan.copy(alpha = 0.42f)

  drawPath(
    Path().apply {
      moveTo(left, top + len)
      lineTo(left, top + radius)
      quadraticTo(left, top, left + radius, top)
      lineTo(left + len, top)
    },
    color = color,
    style = stroke,
  )
  drawPath(
    Path().apply {
      moveTo(right - len, top)
      lineTo(right - radius, top)
      quadraticTo(right, top, right, top + radius)
      lineTo(right, top + len)
    },
    color = color,
    style = stroke,
  )
  drawPath(
    Path().apply {
      moveTo(right, bottom - len)
      lineTo(right, bottom - radius)
      quadraticTo(right, bottom, right - radius, bottom)
      lineTo(right - len, bottom)
    },
    color = color,
    style = stroke,
  )
  drawPath(
    Path().apply {
      moveTo(left + len, bottom)
      lineTo(left + radius, bottom)
      quadraticTo(left, bottom, left, bottom - radius)
      lineTo(left, bottom - len)
    },
    color = color,
    style = stroke,
  )
}
