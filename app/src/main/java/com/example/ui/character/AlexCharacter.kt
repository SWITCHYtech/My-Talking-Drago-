package com.example.ui.character

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AlexBackgroundCharacter(
    isWaving: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "alex_idle")
    val breathPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alex_breath"
    )
    val wavePulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alex_wave"
    )
    val blinkCycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alex_blink"
    )

    Box(
        modifier = modifier
            .testTag("alex_background_character")
            .semantics {
                contentDescription = "Alex, a boy in the living room background with brown hair tied in a ponytail behind him and a dark zip-up jacket"
            }
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val breathY = sin(breathPhase) * h * 0.006f

            // Colors matching Alex from reference photos
            val skinBase = Color(0xFFF3C39C)
            val skinShadow = Color(0xFFDEAD84)
            val hairBrown = Color(0xFF5D3B26)
            val hairHighlight = Color(0xFF7B5238)
            val hairTie = Color(0xFF1F2937)
            val jacketBlack = Color(0xFF111827)
            val jacketShade = Color(0xFF1F2937)
            val zipperSilver = Color(0xFFD1D5DB)
            val innerShirtWhite = Color(0xFFF8FAFC)
            val pantsDark = Color(0xFF334155)

            // Soft ground shadow
            drawOval(
                color = Color(0x2E0F172A),
                topLeft = Offset(cx - w * 0.25f, h * 0.89f),
                size = Size(w * 0.50f, h * 0.055f)
            )

            // ---- LEGS & SHOES ----
            val hipY = h * 0.63f + breathY
            val legH = h * 0.24f
            val legW = w * 0.095f
            drawRoundRect(
                color = pantsDark,
                topLeft = Offset(cx - w * 0.12f, hipY),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(legW * 0.35f, legW * 0.35f)
            )
            drawRoundRect(
                color = pantsDark,
                topLeft = Offset(cx + w * 0.025f, hipY),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(legW * 0.35f, legW * 0.35f)
            )
            // Casual sneakers
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx - w * 0.135f, hipY + legH - h * 0.01f),
                size = Size(w * 0.12f, h * 0.042f),
                cornerRadius = CornerRadius(h * 0.02f, h * 0.02f)
            )
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx + w * 0.015f, hipY + legH - h * 0.01f),
                size = Size(w * 0.12f, h * 0.042f),
                cornerRadius = CornerRadius(h * 0.02f, h * 0.02f)
            )

            // ---- BROWN PONYTAIL TIED BEHIND HIM (Drawn behind shoulders & head) ----
            val headCenterY = h * 0.258f + breathY
            val headRX = w * 0.188f
            val headRY = h * 0.142f

            val ponytailPath = Path().apply {
                moveTo(cx - headRX * 0.35f, headCenterY + headRY * 0.45f)
                cubicTo(
                    cx - headRX * 1.32f, headCenterY + headRY * 0.60f,
                    cx - headRX * 1.78f, headCenterY + headRY * 1.48f,
                    cx - headRX * 1.38f, headCenterY + headRY * 2.15f
                )
                cubicTo(
                    cx - headRX * 1.05f, headCenterY + headRY * 1.92f,
                    cx - headRX * 0.96f, headCenterY + headRY * 1.25f,
                    cx - headRX * 0.30f, headCenterY + headRY * 0.72f
                )
                close()
            }
            drawPath(path = ponytailPath, color = hairBrown)
            drawPath(
                path = ponytailPath,
                color = hairHighlight.copy(alpha = 0.50f),
                style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
            )
            // Hair tie behind neck
            drawCircle(
                color = hairTie,
                radius = w * 0.032f,
                center = Offset(cx - headRX * 0.85f, headCenterY + headRY * 0.65f)
            )

            // ---- ARMS (Dark Zip Jacket Sleeves) ----
            val shoulderY = h * 0.445f + breathY
            val armLen = h * 0.185f
            val armThick = w * 0.064f
            val leftArmAngle = if (isWaving) 120f + sin(wavePulse * 2f) * 20f else 14f + sin(breathPhase) * 1.8f
            val rightArmAngle = -14f - sin(breathPhase) * 1.8f

            rotate(degrees = leftArmAngle, pivot = Offset(cx - w * 0.155f, shoulderY)) {
                drawRoundRect(
                    color = jacketBlack,
                    topLeft = Offset(cx - w * 0.155f - armThick * 0.5f, shoulderY),
                    size = Size(armThick, armLen),
                    cornerRadius = CornerRadius(armThick * 0.45f, armThick * 0.45f)
                )
                drawCircle(
                    color = skinBase,
                    radius = armThick * 0.48f,
                    center = Offset(cx - w * 0.155f, shoulderY + armLen)
                )
            }
            rotate(degrees = rightArmAngle, pivot = Offset(cx + w * 0.155f, shoulderY)) {
                drawRoundRect(
                    color = jacketBlack,
                    topLeft = Offset(cx + w * 0.155f - armThick * 0.5f, shoulderY),
                    size = Size(armThick, armLen),
                    cornerRadius = CornerRadius(armThick * 0.45f, armThick * 0.45f)
                )
                drawCircle(
                    color = skinBase,
                    radius = armThick * 0.48f,
                    center = Offset(cx + w * 0.155f, shoulderY + armLen)
                )
            }

            // ---- DARK ZIP-UP JACKET OVER WHITE T-SHIRT ----
            val torsoTop = h * 0.42f + breathY
            val torsoH = h * 0.23f
            val torsoW = w * 0.34f
            val jacketPath = Path().apply {
                moveTo(cx - torsoW * 0.48f, torsoTop)
                lineTo(cx + torsoW * 0.48f, torsoTop)
                lineTo(cx + torsoW * 0.50f, torsoTop + torsoH)
                lineTo(cx - torsoW * 0.50f, torsoTop + torsoH)
                close()
            }
            drawPath(path = jacketPath, color = jacketBlack)
            drawPath(
                path = jacketPath,
                color = jacketShade,
                style = Stroke(width = w * 0.008f)
            )

            // White inner crew-neck shirt peeking at top of zipper
            val collarPath = Path().apply {
                moveTo(cx - w * 0.056f, torsoTop - h * 0.005f)
                lineTo(cx + w * 0.056f, torsoTop - h * 0.005f)
                lineTo(cx, torsoTop + h * 0.048f)
                close()
            }
            drawPath(path = collarPath, color = innerShirtWhite)

            // Silver vertical zipper down the front
            drawLine(
                color = zipperSilver,
                start = Offset(cx, torsoTop + h * 0.045f),
                end = Offset(cx, torsoTop + torsoH - h * 0.01f),
                strokeWidth = w * 0.013f,
                cap = StrokeCap.Round
            )
            // Silver zipper pull
            drawRoundRect(
                color = zipperSilver,
                topLeft = Offset(cx - w * 0.012f, torsoTop + h * 0.045f),
                size = Size(w * 0.024f, h * 0.024f),
                cornerRadius = CornerRadius(w * 0.006f, w * 0.006f)
            )

            // Neck
            drawRoundRect(
                color = skinShadow,
                topLeft = Offset(cx - w * 0.046f, h * 0.37f + breathY),
                size = Size(w * 0.092f, h * 0.06f),
                cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
            )

            // Pulled-back hair crown behind head
            drawOval(
                color = hairBrown,
                topLeft = Offset(cx - headRX * 1.02f, headCenterY - headRY * 1.12f),
                size = Size(headRX * 2.04f, headRY * 1.62f)
            )

            // Ears
            drawCircle(color = skinBase, radius = w * 0.034f, center = Offset(cx - headRX * 0.96f, headCenterY + headRY * 0.12f))
            drawCircle(color = skinBase, radius = w * 0.034f, center = Offset(cx + headRX * 0.96f, headCenterY + headRY * 0.12f))

            // Face (boyish squared jawline)
            drawRoundRect(
                color = skinBase,
                topLeft = Offset(cx - headRX, headCenterY - headRY),
                size = Size(headRX * 2f, headRY * 1.98f),
                cornerRadius = CornerRadius(headRX * 0.72f, headRY * 0.80f)
            )

            // Smooth pulled-back boyish hairline + short sideburns
            val hairlinePath = Path().apply {
                moveTo(cx - headRX * 0.98f, headCenterY - headRY * 0.12f)
                cubicTo(
                    cx - headRX * 0.95f, headCenterY - headRY * 1.05f,
                    cx + headRX * 0.95f, headCenterY - headRY * 1.05f,
                    cx + headRX * 0.98f, headCenterY - headRY * 0.12f
                )
                quadraticTo(cx, headCenterY - headRY * 0.78f, cx - headRX * 0.98f, headCenterY - headRY * 0.12f)
                close()
            }
            drawPath(path = hairlinePath, color = hairBrown)
            drawRoundRect(
                color = hairBrown,
                topLeft = Offset(cx - headRX * 0.98f, headCenterY - headRY * 0.14f),
                size = Size(headRX * 0.14f, headRY * 0.34f),
                cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
            )
            drawRoundRect(
                color = hairBrown,
                topLeft = Offset(cx + headRX * 0.84f, headCenterY - headRY * 0.14f),
                size = Size(headRX * 0.14f, headRY * 0.34f),
                cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
            )
            drawArc(
                color = hairHighlight.copy(alpha = 0.55f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - headRX * 0.65f, headCenterY - headRY * 0.95f),
                size = Size(headRX * 1.3f, headRY * 0.45f),
                style = Stroke(width = w * 0.015f, cap = StrokeCap.Round)
            )

            // Eyebrows
            val eyeX = headRX * 0.40f
            val eyeY = headCenterY - headRY * 0.04f
            drawLine(
                color = hairBrown,
                start = Offset(cx - eyeX - w * 0.045f, eyeY - h * 0.028f),
                end = Offset(cx - eyeX + w * 0.042f, eyeY - h * 0.030f),
                strokeWidth = w * 0.016f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = hairBrown,
                start = Offset(cx + eyeX - w * 0.042f, eyeY - h * 0.030f),
                end = Offset(cx + eyeX + w * 0.045f, eyeY - h * 0.028f),
                strokeWidth = w * 0.016f,
                cap = StrokeCap.Round
            )

            // Eyes
            val isBlinking = blinkCycle > 0.94f
            val eyeR = w * 0.036f
            listOf(cx - eyeX, cx + eyeX).forEach { ex ->
                if (isBlinking) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(ex - eyeR, eyeY),
                        end = Offset(ex + eyeR, eyeY),
                        strokeWidth = w * 0.012f,
                        cap = StrokeCap.Round
                    )
                } else {
                    drawCircle(color = Color.White, radius = eyeR, center = Offset(ex, eyeY))
                    drawCircle(color = Color(0xFF4A2E1B), radius = eyeR * 0.68f, center = Offset(ex, eyeY))
                    drawCircle(color = Color(0xFF0F172A), radius = eyeR * 0.36f, center = Offset(ex, eyeY))
                    drawCircle(
                        color = Color.White,
                        radius = eyeR * 0.22f,
                        center = Offset(ex - eyeR * 0.22f, eyeY - eyeR * 0.22f)
                    )
                }
            }

            // Nose
            drawArc(
                color = skinShadow,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.02f, headCenterY + headRY * 0.14f),
                size = Size(w * 0.04f, h * 0.022f),
                style = Stroke(width = w * 0.01f, cap = StrokeCap.Round)
            )

            // Calm friendly smile (broader smile when waving!)
            if (isWaving) {
                drawArc(
                    color = Color(0xFF7C2D12),
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.065f, headCenterY + headRY * 0.35f),
                    size = Size(w * 0.13f, h * 0.04f),
                    style = Stroke(width = w * 0.015f, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    color = Color(0xFF7C2D12),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.048f, headCenterY + headRY * 0.42f),
                    size = Size(w * 0.096f, h * 0.022f),
                    style = Stroke(width = w * 0.013f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
