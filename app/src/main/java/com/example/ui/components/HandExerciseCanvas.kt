package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.data.model.HandMotionType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HandExerciseCanvas(
    motionType: HandMotionType,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w * 0.5f
        val cy = h * 0.56f

        // Soft ambient radial backdrop
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryContainer.copy(alpha = 0.7f),
                    primaryContainer.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(cx, cy * 0.92f),
                radius = minDim * 0.48f
            ),
            radius = minDim * 0.48f,
            center = Offset(cx, cy * 0.92f)
        )

        when (motionType) {
            HandMotionType.WRIST_FLEX_EXT -> drawWristFlexSideView(
                cx = cx,
                cy = cy,
                scale = minDim,
                progress = progress,
                primary = primaryColor,
                accent = secondaryColor,
                nodeFill = surfaceColor
            )
            HandMotionType.TABLETOP_FLEX -> drawTabletopSideView(
                cx = cx,
                cy = cy,
                scale = minDim,
                progress = progress,
                primary = primaryColor,
                accent = secondaryColor,
                nodeFill = surfaceColor
            )
            HandMotionType.GRIP_SQUEEZE -> drawGripBallHand(
                cx = cx,
                cy = cy,
                scale = minDim,
                progress = progress,
                primary = primaryColor,
                accent = secondaryColor,
                nodeFill = surfaceColor
            )
            HandMotionType.THUMB_OPPOSITION,
            HandMotionType.PINCH_CONTROL -> drawPinchHand(
                cx = cx,
                cy = cy,
                scale = minDim,
                progress = progress,
                primary = primaryColor,
                accent = secondaryColor,
                nodeFill = surfaceColor
            )
            HandMotionType.FINGER_SPREAD -> drawArticulatedFrontalHand(
                cx = cx,
                cy = cy,
                scale = minDim,
                spreadFactor = progress,
                curlFactor = 0f,
                hookOnly = false,
                primary = primaryColor,
                accent = tertiaryColor,
                nodeFill = surfaceColor
            )
            HandMotionType.HOOK_FIST -> drawArticulatedFrontalHand(
                cx = cx,
                cy = cy,
                scale = minDim,
                spreadFactor = 0.25f,
                curlFactor = progress,
                hookOnly = true,
                primary = primaryColor,
                accent = secondaryColor,
                nodeFill = surfaceColor
            )
            HandMotionType.TENDON_GLIDE -> drawArticulatedFrontalHand(
                cx = cx,
                cy = cy,
                scale = minDim,
                spreadFactor = 0.18f,
                curlFactor = progress,
                hookOnly = progress < 0.6f,
                primary = primaryColor,
                accent = secondaryColor,
                nodeFill = surfaceColor
            )
        }
    }
}

private fun DrawScope.drawArticulatedFrontalHand(
    cx: Float,
    cy: Float,
    scale: Float,
    spreadFactor: Float,
    curlFactor: Float,
    hookOnly: Boolean,
    primary: Color,
    accent: Color,
    nodeFill: Color
) {
    val palmW = scale * 0.23f
    val palmH = scale * 0.24f
    val wristY = cy + palmH * 0.68f

    // Forearm cuff
    drawRoundRect(
        color = primary.copy(alpha = 0.22f),
        topLeft = Offset(cx - palmW * 0.42f, wristY - scale * 0.02f),
        size = Size(palmW * 0.84f, scale * 0.15f),
        cornerRadius = CornerRadius(scale * 0.04f, scale * 0.04f)
    )

    // Palm body
    drawRoundRect(
        color = primary.copy(alpha = 0.85f),
        topLeft = Offset(cx - palmW * 0.5f, cy - palmH * 0.35f),
        size = Size(palmW, palmH),
        cornerRadius = CornerRadius(scale * 0.07f, scale * 0.07f)
    )

    // Motion arc indicator above fingertips
    drawArc(
        color = accent.copy(alpha = 0.35f + spreadFactor * 0.45f),
        startAngle = 210f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(cx - scale * 0.34f, cy - scale * 0.42f),
        size = Size(scale * 0.68f, scale * 0.45f),
        style = Stroke(width = scale * 0.012f, cap = StrokeCap.Round)
    )

    // 4 Fingers (Index, Middle, Ring, Pinky)
    val fingerSpecs = listOf(
        Triple(-0.34f, 0.22f, -15f),
        Triple(-0.11f, 0.25f, -5f),
        Triple(0.12f, 0.23f, 6f),
        Triple(0.34f, 0.18f, 16f)
    )
    val fingerWidth = scale * 0.042f

    fingerSpecs.forEach { (xRatio, lengthRatio, maxSpreadDeg) ->
        val base = Offset(cx + palmW * xRatio, cy - palmH * 0.30f)
        val angleDeg = maxSpreadDeg * spreadFactor
        val rad = (angleDeg - 90f) * (PI / 180.0).toFloat()

        // Shorten apparent vertical reach as curlFactor increases
        val curlCompression = if (hookOnly) 1f - (curlFactor * 0.38f) else 1f - (curlFactor * 0.65f)
        val totalLen = scale * lengthRatio * curlCompression
        val midPoint = Offset(
            x = base.x + cos(rad) * totalLen * 0.52f,
            y = base.y + sin(rad) * totalLen * 0.52f
        )
        val tipPoint = Offset(
            x = base.x + cos(rad) * totalLen,
            y = base.y + sin(rad) * totalLen + (if (curlFactor > 0.3f) curlFactor * scale * 0.025f else 0f)
        )

        drawLine(
            color = primary,
            start = base,
            end = midPoint,
            strokeWidth = fingerWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary.copy(alpha = 0.92f),
            start = midPoint,
            end = tipPoint,
            strokeWidth = fingerWidth * 0.88f,
            cap = StrokeCap.Round
        )

        // Joint landmark nodes
        drawCircle(color = accent, radius = scale * 0.015f, center = base)
        drawCircle(color = nodeFill, radius = scale * 0.011f, center = midPoint)
        drawCircle(color = accent, radius = scale * 0.013f, center = tipPoint)
    }

    // Thumb
    val thumbBase = Offset(cx - palmW * 0.45f, cy + palmH * 0.16f)
    val thumbAngleDeg = -140f - (spreadFactor * 18f) + (curlFactor * 28f)
    val thumbRad = thumbAngleDeg * (PI / 180.0).toFloat()
    val thumbLen = scale * 0.15f
    val thumbTip = Offset(
        x = thumbBase.x + cos(thumbRad) * thumbLen,
        y = thumbBase.y + sin(thumbRad) * thumbLen
    )
    drawLine(
        color = primary,
        start = thumbBase,
        end = thumbTip,
        strokeWidth = fingerWidth * 1.05f,
        cap = StrokeCap.Round
    )
    drawCircle(color = accent, radius = scale * 0.014f, center = thumbTip)
}

private fun DrawScope.drawWristFlexSideView(
    cx: Float,
    cy: Float,
    scale: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    nodeFill: Color
) {
    // Table surface
    val tableY = cy + scale * 0.15f
    drawLine(
        color = primary.copy(alpha = 0.25f),
        start = Offset(cx - scale * 0.42f, tableY),
        end = Offset(cx + scale * 0.15f, tableY),
        strokeWidth = scale * 0.022f,
        cap = StrokeCap.Round
    )

    // Forearm resting on table
    val wristCenter = Offset(cx + scale * 0.02f, cy + scale * 0.07f)
    drawLine(
        color = primary.copy(alpha = 0.8f),
        start = Offset(cx - scale * 0.36f, wristCenter.y),
        end = wristCenter,
        strokeWidth = scale * 0.09f,
        cap = StrokeCap.Round
    )

    // Motion arc guide
    drawArc(
        color = accent.copy(alpha = 0.4f),
        startAngle = -55f,
        sweepAngle = 95f,
        useCenter = false,
        topLeft = Offset(wristCenter.x - scale * 0.18f, wristCenter.y - scale * 0.22f),
        size = Size(scale * 0.44f, scale * 0.44f),
        style = Stroke(width = scale * 0.014f, cap = StrokeCap.Round)
    )

    // Pivoting hand (+35 deg down to -45 deg up)
    val tiltDeg = 28f - (progress * 70f)
    rotate(degrees = tiltDeg, pivot = wristCenter) {
        drawRoundRect(
            color = primary,
            topLeft = Offset(wristCenter.x, wristCenter.y - scale * 0.036f),
            size = Size(scale * 0.27f, scale * 0.072f),
            cornerRadius = CornerRadius(scale * 0.036f, scale * 0.036f)
        )
        drawCircle(
            color = accent,
            radius = scale * 0.018f,
            center = Offset(wristCenter.x + scale * 0.25f, wristCenter.y)
        )
    }

    drawCircle(color = accent, radius = scale * 0.025f, center = wristCenter)
    drawCircle(color = nodeFill, radius = scale * 0.012f, center = wristCenter)
}

private fun DrawScope.drawTabletopSideView(
    cx: Float,
    cy: Float,
    scale: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    nodeFill: Color
) {
    val wrist = Offset(cx - scale * 0.1f, cy + scale * 0.22f)
    val knuckle = Offset(cx - scale * 0.1f, cy - scale * 0.02f)

    // Forearm + vertical metacarpals
    drawLine(
        color = primary.copy(alpha = 0.8f),
        start = Offset(wrist.x, wrist.y + scale * 0.12f),
        end = knuckle,
        strokeWidth = scale * 0.068f,
        cap = StrokeCap.Round
    )

    // Straight fingers bending 0 -> 90 deg at knuckle
    val bendAngleRad = (-90f + progress * 90f) * (PI / 180.0).toFloat()
    val fingerLen = scale * 0.25f
    val tip = Offset(
        x = knuckle.x + cos(bendAngleRad) * fingerLen,
        y = knuckle.y + sin(bendAngleRad) * fingerLen
    )
    drawLine(
        color = primary,
        start = knuckle,
        end = tip,
        strokeWidth = scale * 0.052f,
        cap = StrokeCap.Round
    )

    drawCircle(color = accent, radius = scale * 0.024f, center = knuckle)
    drawCircle(color = nodeFill, radius = scale * 0.012f, center = knuckle)
    drawCircle(color = accent, radius = scale * 0.018f, center = tip)
}

private fun DrawScope.drawGripBallHand(
    cx: Float,
    cy: Float,
    scale: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    nodeFill: Color
) {
    val ballRadius = scale * (0.145f - progress * 0.028f)
    val ballCenter = Offset(cx, cy - scale * 0.02f)

    // Soft therapy ball
    drawCircle(
        color = accent.copy(alpha = 0.88f),
        radius = ballRadius,
        center = ballCenter
    )
    drawCircle(
        color = nodeFill.copy(alpha = 0.35f),
        radius = ballRadius * 0.45f,
        center = Offset(ballCenter.x - ballRadius * 0.28f, ballCenter.y - ballRadius * 0.28f)
    )

    // Fingers wrapping from top/sides
    val wrap = progress * scale * 0.045f
    for (i in -1..2) {
        val offsetX = i * scale * 0.06f - scale * 0.03f
        val start = Offset(cx + offsetX, cy - scale * 0.24f + wrap)
        val end = Offset(cx + offsetX * 0.85f, cy - scale * 0.04f + wrap * 0.6f)
        drawLine(
            color = primary,
            start = start,
            end = end,
            strokeWidth = scale * 0.042f,
            cap = StrokeCap.Round
        )
        drawCircle(color = nodeFill, radius = scale * 0.011f, center = end)
    }

    // Palm base below ball
    drawRoundRect(
        color = primary.copy(alpha = 0.88f),
        topLeft = Offset(cx - scale * 0.13f, cy + scale * 0.09f - wrap * 0.4f),
        size = Size(scale * 0.26f, scale * 0.16f),
        cornerRadius = CornerRadius(scale * 0.06f, scale * 0.06f)
    )
}

private fun DrawScope.drawPinchHand(
    cx: Float,
    cy: Float,
    scale: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    nodeFill: Color
) {
    val wrist = Offset(cx - scale * 0.15f, cy + scale * 0.22f)
    val palmTop = Offset(cx - scale * 0.05f, cy + scale * 0.02f)

    // Wrist & palm silhouette
    drawLine(
        color = primary.copy(alpha = 0.85f),
        start = wrist,
        end = palmTop,
        strokeWidth = scale * 0.12f,
        cap = StrokeCap.Round
    )

    // Upper finger (index) curving into pinch
    val gap = (1f - progress) * scale * 0.09f
    val indexMid = Offset(cx + scale * 0.12f, cy - scale * 0.16f)
    val indexTip = Offset(cx + scale * 0.18f, cy - scale * 0.04f - gap * 0.6f)

    val indexPath = Path().apply {
        moveTo(palmTop.x, palmTop.y - scale * 0.03f)
        quadraticTo(indexMid.x, indexMid.y, indexTip.x, indexTip.y)
    }
    drawPath(
        path = indexPath,
        color = primary,
        style = Stroke(width = scale * 0.045f, cap = StrokeCap.Round)
    )

    // Thumb curving up to meet index tip
    val thumbMid = Offset(cx + scale * 0.06f, cy + scale * 0.12f)
    val thumbTip = Offset(cx + scale * 0.17f, cy + scale * 0.01f + gap * 0.6f)
    val thumbPath = Path().apply {
        moveTo(palmTop.x, palmTop.y + scale * 0.04f)
        quadraticTo(thumbMid.x, thumbMid.y, thumbTip.x, thumbTip.y)
    }
    drawPath(
        path = thumbPath,
        color = primary,
        style = Stroke(width = scale * 0.048f, cap = StrokeCap.Round)
    )

    // Target O-ring glow when pinched
    val pinchCenter = Offset(
        (indexTip.x + thumbTip.x) * 0.5f,
        (indexTip.y + thumbTip.y) * 0.5f
    )
    if (progress > 0.7f) {
        drawCircle(
            color = accent.copy(alpha = 0.45f),
            radius = scale * 0.06f,
            center = pinchCenter
        )
    }
    drawCircle(color = accent, radius = scale * 0.018f, center = indexTip)
    drawCircle(color = accent, radius = scale * 0.018f, center = thumbTip)
    drawCircle(color = nodeFill, radius = scale * 0.009f, center = indexTip)
    drawCircle(color = nodeFill, radius = scale * 0.009f, center = thumbTip)
}
