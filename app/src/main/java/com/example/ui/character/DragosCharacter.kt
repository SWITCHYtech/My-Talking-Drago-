package com.example.ui.character

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.model.BodyZone
import com.example.model.DragosReaction
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DragosCharacter(
    reaction: DragosReaction,
    mouthAmplitude: Float,
    modifier: Modifier = Modifier,
    holdingOnePlus13: Boolean = false,
    femboyMode: Boolean = false,
    onBodyTap: (BodyZone) -> Unit = {},
    onHeadPetting: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "dragos_idle")
    val breathPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )
    val fastPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fast_pulse"
    )
    val blinkCycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    val jumpTarget = when (reaction) {
        DragosReaction.JUMPING -> -0.11f
        DragosReaction.FOOT_HOP -> -0.06f
        DragosReaction.HAPPY_LAUGH, DragosReaction.DANCING, DragosReaction.GOOD_BOY -> -abs(sin(fastPulse)) * 0.035f
        else -> 0f
    }
    val verticalJump by animateFloatAsState(
        targetValue = jumpTarget,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f),
        label = "jump"
    )

    val stretchTargetX = when (reaction) {
        DragosReaction.BELLY_TICKLE -> 1.05f + 0.03f * sin(fastPulse * 2f)
        DragosReaction.JUMPING -> 0.94f
        DragosReaction.FOOT_HOP -> 0.97f
        else -> 1f
    }
    val stretchTargetY = when (reaction) {
        DragosReaction.BELLY_TICKLE -> 0.95f - 0.03f * sin(fastPulse * 2f)
        DragosReaction.JUMPING -> 1.07f
        else -> 1f
    }
    val scaleX by animateFloatAsState(stretchTargetX, spring(dampingRatio = 0.6f), label = "sx")
    val scaleY by animateFloatAsState(stretchTargetY, spring(dampingRatio = 0.6f), label = "sy")

    val smoothMouth by animateFloatAsState(
        targetValue = mouthAmplitude.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 60, easing = FastOutSlowInEasing),
        label = "mouth"
    )

    var lastPetTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .testTag("dragos_character")
            .semantics {
                contentDescription = if (femboyMode) {
                    "Dragoș in femboy mode with cat ears, pink heart glasses, and a pink maid outfit"
                } else {
                    "Dragoș, a boy with fair brown hair, glasses, white shirt, and blue Nike pants"
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val yFrac = offset.y / size.height.coerceAtLeast(1)
                    val zone = when {
                        yFrac < 0.44f -> BodyZone.HEAD
                        yFrac < 0.68f -> BodyZone.BELLY
                        else -> BodyZone.FEET
                    }
                    onBodyTap(zone)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val yFrac = change.position.y / size.height.coerceAtLeast(1)
                    if (yFrac < 0.46f) {
                        val now = System.currentTimeMillis()
                        if (now - lastPetTime > 900L) {
                            lastPetTime = now
                            onHeadPetting()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // Ground shadow
            val shadowScale = (1f + verticalJump * 2.2f).coerceIn(0.65f, 1.05f)
            drawOval(
                color = Color(0x360F172A),
                topLeft = Offset(cx - w * 0.24f * shadowScale, h * 0.895f),
                size = Size(w * 0.48f * shadowScale, h * 0.055f)
            )

            val breathY = sin(breathPhase) * h * 0.007f
            val danceTilt = when (reaction) {
                DragosReaction.DANCING -> sin(fastPulse) * 9f
                DragosReaction.GIGGLE_HEAD -> sin(fastPulse * 2f) * 7f
                DragosReaction.LISTENING -> -5f
                DragosReaction.SPICY_FIRE -> sin(fastPulse * 3f) * 4f
                else -> sin(breathPhase * 0.5f) * 1.2f
            }

            val bodyShiftY = verticalJump * h + breathY
            val pivot = Offset(cx, h * 0.88f)

            scale(scaleX = scaleX, scaleY = scaleY, pivot = pivot) {
                rotate(degrees = danceTilt * 0.45f, pivot = pivot) {
                    // Palette tailored specifically for Dragoș
                    val skinBase = Color(0xFFF6C7A3)
                    val skinShadow = Color(0xFFE5A87E)
                    val blushColor = Color(0x66F43F5E)
                    val hairBase = Color(0xFF8C5A3C)      // Fair brown hair
                    val hairHighlight = Color(0xFFB67E58)
                    val hairShadow = Color(0xFF694028)
                    val shirtWhite = Color(0xFFFFFFFF)    // Crisp white shirt
                    val shirtShadow = Color(0xFFE2E8F0)
                    val nikeBlue = Color(0xFF1D4ED8)      // Blue Nike pants
                    val nikeBlueDark = Color(0xFF1E3A8A)
                    val sneakerBlue = Color(0xFF2563EB)
                    val glassesFrame = Color(0xFF1E293B)
                    val maidPink = Color(0xFFF472B6)
                    val maidPinkLight = Color(0xFFF9A8D4)
                    val maidPinkDark = Color(0xFFDB2777)

                    // ---- LEGS & PANTS / MAID SOCKS ----
                    val hipY = h * 0.625f + bodyShiftY
                    val kneeY = h * 0.77f + bodyShiftY
                    val ankleY = h * 0.86f + bodyShiftY
                    val legSpread = w * 0.072f
                    val legWidth = w * 0.102f

                    val rightLegLift = when (reaction) {
                        DragosReaction.FOOT_HOP -> -h * 0.065f
                        DragosReaction.SOCCER_KICK -> -h * 0.09f
                        DragosReaction.DANCING -> maxOf(0f, sin(fastPulse)) * -h * 0.035f
                        else -> 0f
                    }
                    val leftLegLift = when (reaction) {
                        DragosReaction.DANCING -> maxOf(0f, -sin(fastPulse)) * -h * 0.035f
                        else -> 0f
                    }
                    val kickAngle = if (reaction == DragosReaction.SOCCER_KICK) -28f else 0f

                    if (femboyMode) {
                        // Left Leg with knee-high white sock + Mary Jane shoe
                        drawFemboyLeg(
                            cx = cx - legSpread,
                            hipY = hipY,
                            kneeY = kneeY,
                            ankleY = ankleY + leftLegLift,
                            legWidth = legWidth * 0.86f,
                            skinColor = skinBase,
                            stripePink = maidPink
                        )
                        drawMaryJaneShoe(
                            centerX = cx - legSpread - w * 0.012f,
                            topY = ankleY + leftLegLift - h * 0.004f,
                            width = w * 0.118f,
                            height = h * 0.050f,
                            shoeColor = maidPinkDark,
                            flip = true
                        )

                        // Right Leg (rotates on soccer kick)
                        rotate(degrees = kickAngle, pivot = Offset(cx + legSpread, hipY)) {
                            drawFemboyLeg(
                                cx = cx + legSpread,
                                hipY = hipY,
                                kneeY = kneeY,
                                ankleY = ankleY + rightLegLift,
                                legWidth = legWidth * 0.86f,
                                skinColor = skinBase,
                                stripePink = maidPink
                            )
                            drawMaryJaneShoe(
                                centerX = cx + legSpread + w * 0.012f,
                                topY = ankleY + rightLegLift - h * 0.004f,
                                width = w * 0.118f,
                                height = h * 0.050f,
                                shoeColor = maidPinkDark,
                                flip = false
                            )
                        }
                    } else {
                        // Waistband of Nike pants
                        drawRoundRect(
                            color = nikeBlueDark,
                            topLeft = Offset(cx - w * 0.135f, hipY - h * 0.022f),
                            size = Size(w * 0.27f, h * 0.055f),
                            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                        )

                        // Left Leg
                        drawNikePantLeg(
                            cx = cx - legSpread,
                            hipY = hipY,
                            ankleY = ankleY + leftLegLift,
                            legWidth = legWidth,
                            nikeBlue = nikeBlue,
                            nikeBlueDark = nikeBlueDark,
                            drawSwooshOnThigh = true,
                            isLeft = true
                        )

                        // Right Leg (rotates on soccer kick)
                        rotate(degrees = kickAngle, pivot = Offset(cx + legSpread, hipY)) {
                            drawNikePantLeg(
                                cx = cx + legSpread,
                                hipY = hipY,
                                ankleY = ankleY + rightLegLift,
                                legWidth = legWidth,
                                nikeBlue = nikeBlue,
                                nikeBlueDark = nikeBlueDark,
                                drawSwooshOnThigh = false,
                                isLeft = false
                            )
                            // Right Sneaker
                            drawSneaker(
                                centerX = cx + legSpread + w * 0.015f,
                                topY = ankleY + rightLegLift - h * 0.005f,
                                width = w * 0.125f,
                                height = h * 0.052f,
                                sneakerBlue = sneakerBlue,
                                flip = false
                            )
                        }

                        // Left Sneaker
                        drawSneaker(
                            centerX = cx - legSpread - w * 0.015f,
                            topY = ankleY + leftLegLift - h * 0.005f,
                            width = w * 0.125f,
                            height = h * 0.052f,
                            sneakerBlue = sneakerBlue,
                            flip = true
                        )

                        // White Nike pants drawstrings
                        drawLine(
                            color = Color.White,
                            start = Offset(cx - w * 0.012f, hipY + h * 0.005f),
                            end = Offset(cx - w * 0.022f, hipY + h * 0.045f),
                            strokeWidth = w * 0.01f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(cx + w * 0.012f, hipY + h * 0.005f),
                            end = Offset(cx + w * 0.022f, hipY + h * 0.045f),
                            strokeWidth = w * 0.01f,
                            cap = StrokeCap.Round
                        )
                    }

                    // ---- ARMS (Behind/Side of Torso) ----
                    val shoulderY = h * 0.445f + bodyShiftY
                    val armLength = h * 0.18f
                    val leftShoulder = Offset(cx - w * 0.135f, shoulderY)
                    val rightShoulder = Offset(cx + w * 0.135f, shoulderY)

                    val showPhoneInHand = holdingOnePlus13 && reaction != DragosReaction.SLEEPING

                    val leftArmAngle = when {
                        showPhoneInHand && reaction == DragosReaction.DANCING -> 56f + sin(fastPulse) * 20f
                        showPhoneInHand -> 52f + sin(breathPhase) * 2.5f
                        reaction == DragosReaction.WAVE -> 125f + sin(fastPulse * 2f) * 24f
                        reaction == DragosReaction.DANCING -> 95f + sin(fastPulse) * 45f
                        reaction == DragosReaction.JUMPING || reaction == DragosReaction.HAPPY_LAUGH || reaction == DragosReaction.GOOD_BOY -> 115f
                        reaction == DragosReaction.BELLY_TICKLE -> -38f + sin(fastPulse * 2f) * 10f
                        reaction == DragosReaction.PETTING_PURR -> 35f
                        reaction == DragosReaction.LISTENING -> 14f
                        reaction == DragosReaction.SLEEPING -> 12f
                        else -> 18f + sin(breathPhase) * 2.5f
                    }
                    val rightArmAngle = when {
                        showPhoneInHand && reaction == DragosReaction.WAVE -> -125f + sin(fastPulse * 2f) * 24f
                        reaction == DragosReaction.LISTENING -> -118f // cupping ear!
                        reaction == DragosReaction.DANCING -> -95f + sin(fastPulse) * 45f
                        reaction == DragosReaction.JUMPING || reaction == DragosReaction.HAPPY_LAUGH || reaction == DragosReaction.GOOD_BOY -> -115f
                        reaction == DragosReaction.BELLY_TICKLE -> 38f - sin(fastPulse * 2f) * 10f
                        reaction == DragosReaction.PETTING_PURR -> -35f
                        reaction == DragosReaction.WAVE -> -20f
                        reaction == DragosReaction.SLEEPING -> -12f
                        else -> -18f - sin(breathPhase) * 2.5f
                    }

                    // Left Arm + Sleeve
                    drawArmWithSleeve(
                        shoulder = leftShoulder,
                        angleDeg = leftArmAngle,
                        armLength = armLength,
                        armThickness = w * 0.056f,
                        sleeveColor = if (femboyMode) maidPink else shirtWhite,
                        sleeveShadow = if (femboyMode) maidPinkDark else shirtShadow,
                        skinColor = skinBase,
                        isLeft = true,
                        isMaidSleeve = femboyMode
                    )

                    // Right Arm + Sleeve + Samsung Galaxy Watch on wrist
                    drawArmWithSleeve(
                        shoulder = rightShoulder,
                        angleDeg = rightArmAngle,
                        armLength = armLength,
                        armThickness = w * 0.056f,
                        sleeveColor = if (femboyMode) maidPink else shirtWhite,
                        sleeveShadow = if (femboyMode) maidPinkDark else shirtShadow,
                        skinColor = skinBase,
                        isLeft = false,
                        showSamsungWatch = true,
                        isMaidSleeve = femboyMode
                    )

                    if (showPhoneInHand) {
                        val rad = (leftArmAngle * PI / 180.0).toFloat()
                        val handCenter = Offset(
                            x = leftShoulder.x - sin(rad) * armLength,
                            y = leftShoulder.y + cos(rad) * armLength
                        )
                        drawBlueOnePlus13(
                            handCenter = handCenter,
                            phoneWidth = w * 0.145f,
                            phoneHeight = h * 0.165f,
                            skinColor = skinBase,
                            tiltDeg = -10f + sin(breathPhase) * 2f
                        )
                    }

                    // ---- TORSO (WHITE T-SHIRT OR PINK MAID OUTFIT) ----
                    val torsoTop = h * 0.415f + bodyShiftY
                    val torsoHeight = h * 0.225f
                    val torsoWidth = w * 0.28f

                    if (femboyMode) {
                        drawPinkMaidOutfit(
                            cx = cx,
                            torsoTop = torsoTop,
                            torsoWidth = torsoWidth,
                            torsoHeight = torsoHeight,
                            skirtBottomY = h * 0.725f + bodyShiftY,
                            maidPink = maidPink,
                            maidPinkLight = maidPinkLight,
                            maidPinkDark = maidPinkDark
                        )
                    } else {
                        val shirtPath = Path().apply {
                            moveTo(cx - torsoWidth * 0.44f, torsoTop)
                            quadraticTo(cx, torsoTop + h * 0.02f, cx + torsoWidth * 0.44f, torsoTop)
                            lineTo(cx + torsoWidth * 0.52f, torsoTop + torsoHeight)
                            quadraticTo(cx, torsoTop + torsoHeight + h * 0.015f, cx - torsoWidth * 0.52f, torsoTop + torsoHeight)
                            close()
                        }
                        drawPath(path = shirtPath, color = shirtWhite)
                        drawPath(
                            path = shirtPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, shirtShadow.copy(alpha = 0.65f)),
                                startY = torsoTop + torsoHeight * 0.45f,
                                endY = torsoTop + torsoHeight
                            )
                        )
                        // Subtle outline for crisp cartoon contrast against bright backgrounds
                        drawPath(
                            path = shirtPath,
                            color = Color(0xFFCBD5E1),
                            style = Stroke(width = w * 0.008f, join = StrokeJoin.Round)
                        )

                        // Crew neck collar
                        drawArc(
                            color = Color(0xFFCBD5E1),
                            startAngle = 10f,
                            sweepAngle = 160f,
                            useCenter = false,
                            topLeft = Offset(cx - w * 0.055f, torsoTop - h * 0.012f),
                            size = Size(w * 0.11f, h * 0.036f),
                            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
                        )
                    }

                    // Neck
                    drawRoundRect(
                        color = skinShadow,
                        topLeft = Offset(cx - w * 0.042f, h * 0.365f + bodyShiftY),
                        size = Size(w * 0.084f, h * 0.065f),
                        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                    )

                    // ---- HEAD, FAIR BROWN HAIR & GLASSES ----
                    val headCenterY = h * 0.255f + bodyShiftY
                    val headRadiusX = w * 0.195f
                    val headRadiusY = h * 0.145f
                    val headTilt = when (reaction) {
                        DragosReaction.GIGGLE_HEAD -> sin(fastPulse * 2.5f) * 14f
                        DragosReaction.LISTENING -> -10f
                        DragosReaction.PETTING_PURR -> 7f
                        DragosReaction.SLEEPING -> 6f
                        DragosReaction.EATING_DISGUSTED -> -8f
                        else -> danceTilt * 0.7f
                    }

                    rotate(degrees = headTilt, pivot = Offset(cx, headCenterY + headRadiusY * 0.5f)) {
                        // Back hair volume behind ears
                        drawOval(
                            color = hairShadow,
                            topLeft = Offset(cx - headRadiusX * 1.08f, headCenterY - headRadiusY * 1.15f),
                            size = Size(headRadiusX * 2.16f, headRadiusY * 1.72f)
                        )

                        // Ears
                        drawCircle(
                            color = skinShadow,
                            radius = w * 0.042f,
                            center = Offset(cx - headRadiusX * 0.96f, headCenterY + headRadiusY * 0.12f)
                        )
                        drawCircle(
                            color = skinBase,
                            radius = w * 0.036f,
                            center = Offset(cx - headRadiusX * 0.96f, headCenterY + headRadiusY * 0.12f)
                        )
                        drawCircle(
                            color = skinShadow,
                            radius = w * 0.042f,
                            center = Offset(cx + headRadiusX * 0.96f, headCenterY + headRadiusY * 0.12f)
                        )
                        drawCircle(
                            color = skinBase,
                            radius = w * 0.036f,
                            center = Offset(cx + headRadiusX * 0.96f, headCenterY + headRadiusY * 0.12f)
                        )

                        // Face shape
                        drawOval(
                            color = skinBase,
                            topLeft = Offset(cx - headRadiusX, headCenterY - headRadiusY),
                            size = Size(headRadiusX * 2f, headRadiusY * 2f)
                        )

                        // Warm cheek blush (extra melty blush in femboy mode!)
                        val isFemboyMelting = femboyMode && reaction == DragosReaction.GOOD_BOY
                        val cheekAlpha = when {
                            isFemboyMelting -> 0.92f
                            reaction == DragosReaction.PETTING_PURR ||
                                reaction == DragosReaction.HAPPY_LAUGH ||
                                reaction == DragosReaction.GOOD_BOY -> 0.62f
                            femboyMode -> 0.42f
                            else -> 0.25f
                        }
                        drawOval(
                            color = blushColor.copy(alpha = cheekAlpha),
                            topLeft = Offset(cx - headRadiusX * 0.74f, headCenterY + headRadiusY * 0.21f),
                            size = Size(headRadiusX * 0.40f, headRadiusY * 0.24f)
                        )
                        drawOval(
                            color = blushColor.copy(alpha = cheekAlpha),
                            topLeft = Offset(cx + headRadiusX * 0.34f, headCenterY + headRadiusY * 0.21f),
                            size = Size(headRadiusX * 0.40f, headRadiusY * 0.24f)
                        )
                        if (isFemboyMelting) {
                            val linePink = Color(0xFFEC4899)
                            for (side in listOf(-1f, 1f)) {
                                val baseX = cx + side * headRadiusX * 0.54f
                                val baseY = headCenterY + headRadiusY * 0.33f
                                for (k in -1..1) {
                                    val dx = k * headRadiusX * 0.08f
                                    drawLine(
                                        color = linePink,
                                        start = Offset(baseX + dx - headRadiusX * 0.03f, baseY + headRadiusY * 0.06f),
                                        end = Offset(baseX + dx + headRadiusX * 0.03f, baseY - headRadiusY * 0.06f),
                                        strokeWidth = w * 0.008f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Nose
                        drawArc(
                            color = skinShadow,
                            startAngle = 20f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(cx - w * 0.022f, headCenterY + headRadiusY * 0.14f),
                            size = Size(w * 0.044f, h * 0.024f),
                            style = Stroke(width = w * 0.011f, cap = StrokeCap.Round)
                        )

                        // ---- EXPRESSIVE EYES ----
                        val eyeOffsetX = headRadiusX * 0.41f
                        val eyeCenterY = headCenterY - headRadiusY * 0.04f
                        val isBlinking = blinkCycle > 0.94f
                        val eyesClosedHappy = reaction == DragosReaction.HAPPY_LAUGH ||
                            reaction == DragosReaction.BELLY_TICKLE ||
                            reaction == DragosReaction.PETTING_PURR ||
                            reaction == DragosReaction.GOOD_BOY
                        val eyesClosedSleep = reaction == DragosReaction.SLEEPING

                        drawDragosEye(
                            center = Offset(cx - eyeOffsetX, eyeCenterY),
                            radius = w * 0.046f,
                            isBlinking = isBlinking,
                            isHappyCrescent = eyesClosedHappy,
                            isSleeping = eyesClosedSleep,
                            isDisgusted = reaction == DragosReaction.EATING_DISGUSTED,
                            isSurprised = reaction == DragosReaction.SPICY_FIRE || reaction == DragosReaction.LISTENING
                        )
                        drawDragosEye(
                            center = Offset(cx + eyeOffsetX, eyeCenterY),
                            radius = w * 0.046f,
                            isBlinking = isBlinking,
                            isHappyCrescent = eyesClosedHappy,
                            isSleeping = eyesClosedSleep,
                            isDisgusted = reaction == DragosReaction.EATING_DISGUSTED,
                            isSurprised = reaction == DragosReaction.SPICY_FIRE || reaction == DragosReaction.LISTENING
                        )

                        // Eyebrows
                        val browLift = if (reaction == DragosReaction.LISTENING || reaction == DragosReaction.SPICY_FIRE) -h * 0.012f else 0f
                        drawArc(
                            color = hairShadow,
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(cx - eyeOffsetX - w * 0.055f, eyeCenterY - w * 0.095f + browLift),
                            size = Size(w * 0.11f, h * 0.025f),
                            style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = hairShadow,
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(cx + eyeOffsetX - w * 0.055f, eyeCenterY - w * 0.095f + browLift),
                            size = Size(w * 0.11f, h * 0.025f),
                            style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
                        )

                        // ---- STYLISH GLASSES (ROUND OR PINK HEART GLASSES) ----
                        val lensRadius = w * 0.072f
                        val frameStroke = w * 0.017f
                        if (femboyMode) {
                            val heartPink = Color(0xFFEC4899)
                            val heartTint = Color(0x36F472B6)
                            drawHeartLens(
                                center = Offset(cx - eyeOffsetX, eyeCenterY),
                                radius = lensRadius * 1.12f,
                                tintColor = heartTint,
                                frameColor = heartPink,
                                frameStroke = frameStroke
                            )
                            drawHeartLens(
                                center = Offset(cx + eyeOffsetX, eyeCenterY),
                                radius = lensRadius * 1.12f,
                                tintColor = heartTint,
                                frameColor = heartPink,
                                frameStroke = frameStroke
                            )
                            // Pink nose bridge
                            drawArc(
                                color = heartPink,
                                startAngle = 200f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(cx - (eyeOffsetX - lensRadius), eyeCenterY - h * 0.012f),
                                size = Size((eyeOffsetX - lensRadius) * 2f, h * 0.022f),
                                style = Stroke(width = frameStroke * 0.9f, cap = StrokeCap.Round)
                            )
                            // Pink temple arms to ears
                            drawLine(
                                color = heartPink,
                                start = Offset(cx - eyeOffsetX - lensRadius * 1.05f, eyeCenterY - h * 0.010f),
                                end = Offset(cx - headRadiusX * 0.96f, headCenterY + headRadiusY * 0.06f),
                                strokeWidth = frameStroke * 0.85f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = heartPink,
                                start = Offset(cx + eyeOffsetX + lensRadius * 1.05f, eyeCenterY - h * 0.010f),
                                end = Offset(cx + headRadiusX * 0.96f, headCenterY + headRadiusY * 0.06f),
                                strokeWidth = frameStroke * 0.85f,
                                cap = StrokeCap.Round
                            )
                        } else {
                            // Subtle translucent glass tint
                            drawCircle(
                                color = Color(0x28BAE6FD),
                                radius = lensRadius,
                                center = Offset(cx - eyeOffsetX, eyeCenterY)
                            )
                            drawCircle(
                                color = Color(0x28BAE6FD),
                                radius = lensRadius,
                                center = Offset(cx + eyeOffsetX, eyeCenterY)
                            )
                            // Lens shine glint
                            drawLine(
                                color = Color(0x80FFFFFF),
                                start = Offset(cx - eyeOffsetX - lensRadius * 0.45f, eyeCenterY - lensRadius * 0.42f),
                                end = Offset(cx - eyeOffsetX - lensRadius * 0.12f, eyeCenterY - lensRadius * 0.65f),
                                strokeWidth = w * 0.01f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0x80FFFFFF),
                                start = Offset(cx + eyeOffsetX - lensRadius * 0.45f, eyeCenterY - lensRadius * 0.42f),
                                end = Offset(cx + eyeOffsetX - lensRadius * 0.12f, eyeCenterY - lensRadius * 0.65f),
                                strokeWidth = w * 0.01f,
                                cap = StrokeCap.Round
                            )
                            // Left & Right rims
                            drawCircle(
                                color = glassesFrame,
                                radius = lensRadius,
                                center = Offset(cx - eyeOffsetX, eyeCenterY),
                                style = Stroke(width = frameStroke)
                            )
                            drawCircle(
                                color = glassesFrame,
                                radius = lensRadius,
                                center = Offset(cx + eyeOffsetX, eyeCenterY),
                                style = Stroke(width = frameStroke)
                            )
                            // Nose bridge
                            drawArc(
                                color = glassesFrame,
                                startAngle = 200f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(cx - (eyeOffsetX - lensRadius), eyeCenterY - h * 0.012f),
                                size = Size((eyeOffsetX - lensRadius) * 2f, h * 0.022f),
                                style = Stroke(width = frameStroke * 0.9f, cap = StrokeCap.Round)
                            )
                            // Temple arms to ears
                            drawLine(
                                color = glassesFrame,
                                start = Offset(cx - eyeOffsetX - lensRadius, eyeCenterY - h * 0.005f),
                                end = Offset(cx - headRadiusX * 0.96f, headCenterY + headRadiusY * 0.06f),
                                strokeWidth = frameStroke * 0.85f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = glassesFrame,
                                start = Offset(cx + eyeOffsetX + lensRadius, eyeCenterY - h * 0.005f),
                                end = Offset(cx + headRadiusX * 0.96f, headCenterY + headRadiusY * 0.06f),
                                strokeWidth = frameStroke * 0.85f,
                                cap = StrokeCap.Round
                            )
                        }

                        // ---- EXPRESSIVE MOUTH ----
                        drawDragosMouth(
                            center = Offset(cx, headCenterY + headRadiusY * 0.52f),
                            width = w * 0.13f,
                            height = h * 0.058f,
                            reaction = reaction,
                            talkAmplitude = smoothMouth,
                            fastPulse = fastPulse
                        )

                        // ---- FAIR BROWN HAIR (Layered messy top & bangs) ----
                        val hairPath = Path().apply {
                            moveTo(cx - headRadiusX * 1.02f, headCenterY - headRadiusY * 0.12f)
                            cubicTo(
                                cx - headRadiusX * 1.15f, headCenterY - headRadiusY * 0.95f,
                                cx - headRadiusX * 0.45f, headCenterY - headRadiusY * 1.38f,
                                cx + headRadiusX * 0.08f, headCenterY - headRadiusY * 1.28f
                            )
                            // Playful messy hair spikes on top
                            lineTo(cx + headRadiusX * 0.24f, headCenterY - headRadiusY * 1.44f)
                            lineTo(cx + headRadiusX * 0.42f, headCenterY - headRadiusY * 1.24f)
                            lineTo(cx + headRadiusX * 0.68f, headCenterY - headRadiusY * 1.32f)
                            cubicTo(
                                cx + headRadiusX * 1.15f, headCenterY - headRadiusY * 0.92f,
                                cx + headRadiusX * 1.12f, headCenterY - headRadiusY * 0.35f,
                                cx + headRadiusX * 1.02f, headCenterY - headRadiusY * 0.10f
                            )
                            // Forehead sweeping bangs
                            quadraticTo(
                                cx + headRadiusX * 0.72f, headCenterY - headRadiusY * 0.52f,
                                cx + headRadiusX * 0.30f, headCenterY - headRadiusY * 0.44f
                            )
                            lineTo(cx + headRadiusX * 0.14f, headCenterY - headRadiusY * 0.62f)
                            quadraticTo(
                                cx - headRadiusX * 0.24f, headCenterY - headRadiusY * 0.36f,
                                cx - headRadiusX * 0.56f, headCenterY - headRadiusY * 0.54f
                            )
                            quadraticTo(
                                cx - headRadiusX * 0.85f, headCenterY - headRadiusY * 0.48f,
                                cx - headRadiusX * 1.02f, headCenterY - headRadiusY * 0.12f
                            )
                            close()
                        }
                        drawPath(path = hairPath, color = hairBase)

                        // Fair brown hair highlight swoosh
                        val highlightPath = Path().apply {
                            moveTo(cx - headRadiusX * 0.55f, headCenterY - headRadiusY * 0.96f)
                            quadraticTo(
                                cx, headCenterY - headRadiusY * 1.15f,
                                cx + headRadiusX * 0.52f, headCenterY - headRadiusY * 0.92f
                            )
                            quadraticTo(
                                cx, headCenterY - headRadiusY * 0.98f,
                                cx - headRadiusX * 0.55f, headCenterY - headRadiusY * 0.96f
                            )
                            close()
                        }
                        drawPath(path = highlightPath, color = hairHighlight.copy(alpha = 0.75f))

                        // ---- CAT EARS HEADBAND (FEMBOY MODE) ----
                        if (femboyMode) {
                            drawCatEarsHeadband(
                                cx = cx,
                                headCenterY = headCenterY,
                                headRadiusX = headRadiusX,
                                headRadiusY = headRadiusY,
                                outerPink = maidPink,
                                darkPink = maidPinkDark
                            )
                        }

                        // Dizzy reaction stars if head poked
                        if (reaction == DragosReaction.GIGGLE_HEAD) {
                            for (i in 0..2) {
                                val angle = fastPulse + i * (2f * PI / 3f).toFloat()
                                val sx = cx + cos(angle) * headRadiusX * 0.85f
                                val sy = headCenterY - headRadiusY * 1.32f + sin(angle) * h * 0.02f
                                drawCircle(
                                    color = Color(0xFFFACC15),
                                    radius = w * 0.018f,
                                    center = Offset(sx, sy)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawNikePantLeg(
    cx: Float,
    hipY: Float,
    ankleY: Float,
    legWidth: Float,
    nikeBlue: Color,
    nikeBlueDark: Color,
    drawSwooshOnThigh: Boolean,
    isLeft: Boolean
) {
    val legHeight = (ankleY - hipY).coerceAtLeast(10f)
    drawRoundRect(
        color = nikeBlue,
        topLeft = Offset(cx - legWidth * 0.5f, hipY),
        size = Size(legWidth, legHeight),
        cornerRadius = CornerRadius(legWidth * 0.35f, legWidth * 0.35f)
    )
    // Athletic white side stripe
    val stripeX = if (isLeft) cx - legWidth * 0.40f else cx + legWidth * 0.33f
    drawRoundRect(
        color = Color(0xD9FFFFFF),
        topLeft = Offset(stripeX, hipY + legHeight * 0.08f),
        size = Size(legWidth * 0.09f, legHeight * 0.80f),
        cornerRadius = CornerRadius(legWidth * 0.04f, legWidth * 0.04f)
    )
    // Ribbed ankle cuff
    drawRoundRect(
        color = nikeBlueDark,
        topLeft = Offset(cx - legWidth * 0.46f, ankleY - legHeight * 0.11f),
        size = Size(legWidth * 0.92f, legHeight * 0.12f),
        cornerRadius = CornerRadius(legWidth * 0.2f, legWidth * 0.2f)
    )

    // Signature white athletic swoosh emblem on thigh
    if (drawSwooshOnThigh) {
        val sx = cx - legWidth * 0.22f
        val sy = hipY + legHeight * 0.28f
        val sw = legWidth * 0.56f
        val sh = legHeight * 0.14f
        val swoosh = Path().apply {
            moveTo(sx + sw * 0.08f, sy + sh * 0.35f)
            quadraticTo(sx - sw * 0.05f, sy + sh * 0.95f, sx + sw * 0.36f, sy + sh * 0.72f)
            lineTo(sx + sw * 0.96f, sy + sh * 0.05f)
            lineTo(sx + sw * 0.32f, sy + sh * 0.50f)
            quadraticTo(sx + sw * 0.10f, sy + sh * 0.62f, sx + sw * 0.08f, sy + sh * 0.35f)
            close()
        }
        drawPath(path = swoosh, color = Color.White)
    }
}

private fun DrawScope.drawSneaker(
    centerX: Float,
    topY: Float,
    width: Float,
    height: Float,
    sneakerBlue: Color,
    flip: Boolean
) {
    val dir = if (flip) -1f else 1f
    val left = centerX - width * 0.5f
    // Shoe upper
    drawRoundRect(
        color = sneakerBlue,
        topLeft = Offset(left, topY),
        size = Size(width, height * 0.78f),
        cornerRadius = CornerRadius(height * 0.45f, height * 0.45f)
    )
    // Crisp white sole
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(left - width * 0.03f, topY + height * 0.58f),
        size = Size(width * 1.06f, height * 0.42f),
        cornerRadius = CornerRadius(height * 0.25f, height * 0.25f)
    )
    // Small white lace accent
    drawLine(
        color = Color.White,
        start = Offset(centerX - dir * width * 0.05f, topY + height * 0.24f),
        end = Offset(centerX + dir * width * 0.18f, topY + height * 0.34f),
        strokeWidth = height * 0.14f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawArmWithSleeve(
    shoulder: Offset,
    angleDeg: Float,
    armLength: Float,
    armThickness: Float,
    sleeveColor: Color,
    sleeveShadow: Color,
    skinColor: Color,
    isLeft: Boolean,
    showSamsungWatch: Boolean = false,
    isMaidSleeve: Boolean = false
) {
    rotate(degrees = angleDeg, pivot = shoulder) {
        // Forearm & skin
        drawRoundRect(
            color = skinColor,
            topLeft = Offset(shoulder.x - armThickness * 0.45f, shoulder.y),
            size = Size(armThickness * 0.9f, armLength),
            cornerRadius = CornerRadius(armThickness * 0.45f, armThickness * 0.45f)
        )
        // Hand
        drawCircle(
            color = skinColor,
            radius = armThickness * 0.58f,
            center = Offset(shoulder.x, shoulder.y + armLength)
        )
        // Samsung Galaxy Watch on wrist
        if (showSamsungWatch) {
            val wristY = shoulder.y + armLength - armThickness * 0.92f
            val watchCenter = Offset(shoulder.x, wristY)
            val watchR = armThickness * 0.58f
            // Dark silicone sport band wrapped around wrist
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(shoulder.x - armThickness * 0.56f, wristY - armThickness * 0.30f),
                size = Size(armThickness * 1.12f, armThickness * 0.60f),
                cornerRadius = CornerRadius(armThickness * 0.18f, armThickness * 0.18f)
            )
            // Watch side crown buttons (Samsung dual side keys)
            drawRoundRect(
                color = Color(0xFF94A3B8),
                topLeft = Offset(shoulder.x + watchR * 0.85f, wristY - watchR * 0.45f),
                size = Size(watchR * 0.28f, watchR * 0.28f),
                cornerRadius = CornerRadius(watchR * 0.08f, watchR * 0.08f)
            )
            drawRoundRect(
                color = Color(0xFF94A3B8),
                topLeft = Offset(shoulder.x + watchR * 0.85f, wristY + watchR * 0.15f),
                size = Size(watchR * 0.28f, watchR * 0.28f),
                cornerRadius = CornerRadius(watchR * 0.08f, watchR * 0.08f)
            )
            // Circular Galaxy Watch metallic bezel & dark AMOLED face
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = watchR,
                center = watchCenter
            )
            drawCircle(
                color = Color(0xFF0F172A),
                radius = watchR * 0.82f,
                center = watchCenter
            )
            // Samsung One UI Watch cyan & mint health arcs
            drawArc(
                color = Color(0xFF38BDF8),
                startAngle = -90f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(watchCenter.x - watchR * 0.62f, watchCenter.y - watchR * 0.62f),
                size = Size(watchR * 1.24f, watchR * 1.24f),
                style = Stroke(width = watchR * 0.15f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF10B981),
                startAngle = 150f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(watchCenter.x - watchR * 0.62f, watchCenter.y - watchR * 0.62f),
                size = Size(watchR * 1.24f, watchR * 1.24f),
                style = Stroke(width = watchR * 0.15f, cap = StrokeCap.Round)
            )
            // Crisp white watch hands
            drawLine(
                color = Color.White,
                start = watchCenter,
                end = Offset(watchCenter.x - watchR * 0.34f, watchCenter.y - watchR * 0.26f),
                strokeWidth = watchR * 0.14f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = watchCenter,
                end = Offset(watchCenter.x + watchR * 0.38f, watchCenter.y - watchR * 0.34f),
                strokeWidth = watchR * 0.12f,
                cap = StrokeCap.Round
            )
        }
        // Short white t-shirt sleeve
        val sleeveLen = armLength * 0.42f
        val sx = if (isLeft) shoulder.x - armThickness * 0.62f else shoulder.x - armThickness * 0.55f
        drawRoundRect(
            color = sleeveColor,
            topLeft = Offset(sx, shoulder.y - armThickness * 0.35f),
            size = Size(armThickness * 1.18f, sleeveLen),
            cornerRadius = CornerRadius(armThickness * 0.35f, armThickness * 0.35f)
        )
        drawRoundRect(
            color = sleeveShadow,
            topLeft = Offset(sx, shoulder.y - armThickness * 0.35f),
            size = Size(armThickness * 1.18f, sleeveLen),
            cornerRadius = CornerRadius(armThickness * 0.35f, armThickness * 0.35f),
            style = Stroke(width = armThickness * 0.12f)
        )
        if (isMaidSleeve) {
            // Crisp white lace cuff on maid puff sleeve
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(sx - armThickness * 0.06f, shoulder.y - armThickness * 0.35f + sleeveLen * 0.78f),
                size = Size(armThickness * 1.30f, sleeveLen * 0.24f),
                cornerRadius = CornerRadius(armThickness * 0.22f, armThickness * 0.22f)
            )
        }
    }
}

private fun DrawScope.drawDragosEye(
    center: Offset,
    radius: Float,
    isBlinking: Boolean,
    isHappyCrescent: Boolean,
    isSleeping: Boolean,
    isDisgusted: Boolean,
    isSurprised: Boolean
) {
    when {
        isSleeping || isBlinking -> {
            drawArc(
                color = Color(0xFF334155),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius * 0.3f),
                size = Size(radius * 2f, radius * 0.9f),
                style = Stroke(width = radius * 0.24f, cap = StrokeCap.Round)
            )
        }
        isHappyCrescent -> {
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius * 0.5f),
                size = Size(radius * 2f, radius * 1.1f),
                style = Stroke(width = radius * 0.26f, cap = StrokeCap.Round)
            )
        }
        isDisgusted -> {
            // > < wince
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(center.x - radius * 0.7f, center.y - radius * 0.5f),
                end = Offset(center.x + radius * 0.6f, center.y),
                strokeWidth = radius * 0.24f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(center.x + radius * 0.6f, center.y),
                end = Offset(center.x - radius * 0.7f, center.y + radius * 0.5f),
                strokeWidth = radius * 0.24f,
                cap = StrokeCap.Round
            )
        }
        else -> {
            val scale = if (isSurprised) 1.15f else 1f
            drawCircle(color = Color.White, radius = radius * scale, center = center)
            // Warm hazel-brown iris
            drawCircle(
                color = Color(0xFF6B4226),
                radius = radius * 0.64f * scale,
                center = center
            )
            drawCircle(
                color = Color(0xFF0F172A),
                radius = radius * 0.36f * scale,
                center = center
            )
            // Catchlight sparkle
            drawCircle(
                color = Color.White,
                radius = radius * 0.22f,
                center = Offset(center.x - radius * 0.24f, center.y - radius * 0.26f)
            )
        }
    }
}

private fun DrawScope.drawDragosMouth(
    center: Offset,
    width: Float,
    height: Float,
    reaction: DragosReaction,
    talkAmplitude: Float,
    fastPulse: Float
) {
    val mouthColor = Color(0xFF991B1B)
    val tongueColor = Color(0xFFFB7185)

    when {
        reaction == DragosReaction.TALKING -> {
            val openH = (height * (0.28f + talkAmplitude * 1.1f)).coerceAtLeast(height * 0.22f)
            drawOval(
                color = mouthColor,
                topLeft = Offset(center.x - width * 0.38f, center.y - openH * 0.45f),
                size = Size(width * 0.76f, openH)
            )
            drawOval(
                color = tongueColor,
                topLeft = Offset(center.x - width * 0.20f, center.y + openH * 0.05f),
                size = Size(width * 0.40f, openH * 0.40f)
            )
        }
        reaction == DragosReaction.EATING_YUMMY -> {
            val chomp = (0.35f + 0.55f * abs(sin(fastPulse * 2.2f)))
            drawOval(
                color = mouthColor,
                topLeft = Offset(center.x - width * 0.36f, center.y - height * 0.35f * chomp),
                size = Size(width * 0.72f, height * 0.9f * chomp)
            )
            drawOval(
                color = tongueColor,
                topLeft = Offset(center.x - width * 0.2f, center.y),
                size = Size(width * 0.4f, height * 0.35f * chomp)
            )
        }
        reaction == DragosReaction.SPICY_FIRE -> {
            drawOval(
                color = mouthColor,
                topLeft = Offset(center.x - width * 0.30f, center.y - height * 0.35f),
                size = Size(width * 0.60f, height * 0.95f)
            )
            drawOval(
                color = Color(0xFFF97316),
                topLeft = Offset(center.x - width * 0.16f, center.y - height * 0.05f),
                size = Size(width * 0.32f, height * 0.45f)
            )
        }
        reaction == DragosReaction.EATING_DISGUSTED -> {
            val wavy = Path().apply {
                moveTo(center.x - width * 0.42f, center.y + height * 0.12f)
                quadraticTo(center.x - width * 0.2f, center.y - height * 0.22f, center.x, center.y + height * 0.08f)
                quadraticTo(center.x + width * 0.2f, center.y + height * 0.30f, center.x + width * 0.42f, center.y - height * 0.05f)
            }
            drawPath(
                path = wavy,
                color = Color(0xFF7F1D1D),
                style = Stroke(width = height * 0.18f, cap = StrokeCap.Round)
            )
        }
        reaction == DragosReaction.SLEEPING -> {
            val open = 0.35f + 0.25f * sin(fastPulse * 0.4f)
            drawOval(
                color = mouthColor,
                topLeft = Offset(center.x - width * 0.14f, center.y - height * 0.15f),
                size = Size(width * 0.28f, height * 0.45f * open + height * 0.18f)
            )
        }
        reaction == DragosReaction.HAPPY_LAUGH ||
            reaction == DragosReaction.BELLY_TICKLE ||
            reaction == DragosReaction.GIGGLE_HEAD ||
            reaction == DragosReaction.DANCING ||
            reaction == DragosReaction.GOOD_BOY -> {
            // Big open joyful laugh
            val rect = Rect(
                left = center.x - width * 0.48f,
                top = center.y - height * 0.45f,
                right = center.x + width * 0.48f,
                bottom = center.y + height * 0.72f
            )
            drawArc(
                color = mouthColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = rect.topLeft,
                size = rect.size
            )
            // White upper teeth strip
            drawArc(
                color = Color.White,
                startAngle = 8f,
                sweepAngle = 164f,
                useCenter = false,
                topLeft = Offset(rect.left + width * 0.06f, rect.top + height * 0.48f),
                size = Size(rect.width - width * 0.12f, height * 0.25f),
                style = Stroke(width = height * 0.18f, cap = StrokeCap.Round)
            )
        }
        else -> {
            // Warm friendly smile
            drawArc(
                color = Color(0xFF7C2D12),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(center.x - width * 0.42f, center.y - height * 0.32f),
                size = Size(width * 0.84f, height * 0.68f),
                style = Stroke(width = height * 0.18f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawBlueOnePlus13(
    handCenter: Offset,
    phoneWidth: Float,
    phoneHeight: Float,
    skinColor: Color,
    tiltDeg: Float
) {
    val left = handCenter.x - phoneWidth * 0.52f
    val top = handCenter.y - phoneHeight * 0.62f

    rotate(degrees = tiltDeg, pivot = handCenter) {
        // Subtle drop shadow
        drawRoundRect(
            color = Color(0x380F172A),
            topLeft = Offset(left + phoneWidth * 0.05f, top + phoneHeight * 0.03f),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(phoneWidth * 0.16f, phoneWidth * 0.16f)
        )

        // Metallic silver frame
        drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(left - phoneWidth * 0.03f, top - phoneHeight * 0.015f),
            size = Size(phoneWidth * 1.06f, phoneHeight * 1.03f),
            cornerRadius = CornerRadius(phoneWidth * 0.17f, phoneWidth * 0.17f)
        )

        // Midnight Ocean blue vegan-leather back
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF284D8E), Color(0xFF1E3E78), Color(0xFF193466)),
                startY = top,
                endY = top + phoneHeight
            ),
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(phoneWidth * 0.15f, phoneWidth * 0.15f)
        )

        // Iconic circular silver camera island in upper-left
        val camRadius = phoneWidth * 0.30f
        val camCenter = Offset(left + phoneWidth * 0.37f, top + phoneHeight * 0.23f)

        // Horizontal silver accent bar extending from camera island to right edge
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(camCenter.x + camRadius * 0.85f, camCenter.y),
            end = Offset(left + phoneWidth, camCenter.y),
            strokeWidth = phoneHeight * 0.012f
        )

        // Hasselblad-style slanted 'H' mark to the right of camera ring
        val hx = left + phoneWidth * 0.81f
        val hy = camCenter.y - phoneHeight * 0.065f
        val hs = phoneWidth * 0.07f
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(hx - hs * 0.45f, hy - hs * 0.6f),
            end = Offset(hx - hs * 0.65f, hy + hs * 0.6f),
            strokeWidth = phoneWidth * 0.025f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(hx + hs * 0.55f, hy - hs * 0.6f),
            end = Offset(hx + hs * 0.35f, hy + hs * 0.6f),
            strokeWidth = phoneWidth * 0.025f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(hx - hs * 0.55f, hy),
            end = Offset(hx + hs * 0.45f, hy),
            strokeWidth = phoneWidth * 0.022f,
            cap = StrokeCap.Round
        )

        // Silver camera island disc & metallic rim
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFFE2E8F0)),
                start = Offset(camCenter.x - camRadius, camCenter.y - camRadius),
                end = Offset(camCenter.x + camRadius, camCenter.y + camRadius)
            ),
            radius = camRadius,
            center = camCenter
        )
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = camRadius,
            center = camCenter,
            style = Stroke(width = phoneWidth * 0.02f)
        )

        // 2x2 lenses inside circular island (3 dark lenses + 1 bottom-right flash)
        val lensOffset = camRadius * 0.41f
        val lensRadius = camRadius * 0.26f
        val lensCenters = listOf(
            Offset(camCenter.x - lensOffset, camCenter.y - lensOffset),
            Offset(camCenter.x + lensOffset, camCenter.y - lensOffset),
            Offset(camCenter.x - lensOffset, camCenter.y + lensOffset)
        )
        lensCenters.forEach { lc ->
            drawCircle(color = Color(0xFF1E293B), radius = lensRadius, center = lc)
            drawCircle(color = Color(0xFF0F172A), radius = lensRadius * 0.58f, center = lc)
            drawCircle(
                color = Color(0xFF60A5FA),
                radius = lensRadius * 0.18f,
                center = Offset(lc.x - lensRadius * 0.18f, lc.y - lensRadius * 0.18f)
            )
        }
        // Flash module (bottom-right)
        val flashCenter = Offset(camCenter.x + lensOffset, camCenter.y + lensOffset)
        drawCircle(color = Color(0xFF1E293B), radius = lensRadius, center = flashCenter)
        drawCircle(color = Color(0xFFF8FAFC), radius = lensRadius * 0.52f, center = flashCenter)

        // Subtle '1+' centered emblem on lower half of back
        val logoSize = phoneWidth * 0.15f
        val logoLeft = left + phoneWidth * 0.5f - logoSize * 0.5f
        val logoTop = top + phoneHeight * 0.52f
        val logoColor = Color(0x9993C5FD)
        drawRect(
            color = logoColor,
            topLeft = Offset(logoLeft, logoTop),
            size = Size(logoSize, logoSize),
            style = Stroke(width = phoneWidth * 0.022f)
        )
        // '1' inside square
        drawLine(
            color = logoColor,
            start = Offset(logoLeft + logoSize * 0.5f, logoTop + logoSize * 0.24f),
            end = Offset(logoLeft + logoSize * 0.5f, logoTop + logoSize * 0.78f),
            strokeWidth = phoneWidth * 0.022f,
            cap = StrokeCap.Round
        )
        // '+' on top-right corner
        val plusCx = logoLeft + logoSize
        val plusCy = logoTop
        drawLine(
            color = logoColor,
            start = Offset(plusCx - logoSize * 0.22f, plusCy),
            end = Offset(plusCx + logoSize * 0.22f, plusCy),
            strokeWidth = phoneWidth * 0.022f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = logoColor,
            start = Offset(plusCx, plusCy - logoSize * 0.22f),
            end = Offset(plusCx, plusCy + logoSize * 0.22f),
            strokeWidth = phoneWidth * 0.022f,
            cap = StrokeCap.Round
        )

        // Thumb gripping the side of the phone
        drawOval(
            color = skinColor,
            topLeft = Offset(left + phoneWidth * 0.76f, handCenter.y - phoneHeight * 0.08f),
            size = Size(phoneWidth * 0.34f, phoneHeight * 0.16f)
        )
    }
}

private fun DrawScope.drawFemboyLeg(
    cx: Float,
    hipY: Float,
    kneeY: Float,
    ankleY: Float,
    legWidth: Float,
    skinColor: Color,
    stripePink: Color
) {
    val totalLen = (ankleY - hipY).coerceAtLeast(10f)
    val sockStartY = kneeY - totalLen * 0.08f
    // Bare thigh & knee
    drawRoundRect(
        color = skinColor,
        topLeft = Offset(cx - legWidth * 0.48f, hipY),
        size = Size(legWidth * 0.96f, (sockStartY - hipY + legWidth * 0.3f).coerceAtLeast(8f)),
        cornerRadius = CornerRadius(legWidth * 0.35f, legWidth * 0.35f)
    )
    // Knee-high white sock
    val sockLen = (ankleY - sockStartY).coerceAtLeast(8f)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(cx - legWidth * 0.46f, sockStartY),
        size = Size(legWidth * 0.92f, sockLen),
        cornerRadius = CornerRadius(legWidth * 0.22f, legWidth * 0.22f)
    )
    // Cute pink stripes at top of sock
    drawRect(
        color = stripePink,
        topLeft = Offset(cx - legWidth * 0.46f, sockStartY + sockLen * 0.08f),
        size = Size(legWidth * 0.92f, sockLen * 0.07f)
    )
    drawRect(
        color = stripePink,
        topLeft = Offset(cx - legWidth * 0.46f, sockStartY + sockLen * 0.20f),
        size = Size(legWidth * 0.92f, sockLen * 0.05f)
    )
}

private fun DrawScope.drawMaryJaneShoe(
    centerX: Float,
    topY: Float,
    width: Float,
    height: Float,
    shoeColor: Color,
    flip: Boolean
) {
    val left = centerX - width * 0.5f
    drawRoundRect(
        color = shoeColor,
        topLeft = Offset(left, topY),
        size = Size(width, height * 0.85f),
        cornerRadius = CornerRadius(height * 0.45f, height * 0.45f)
    )
    // White inner collar & strap
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(centerX - width * 0.26f, topY + height * 0.10f),
        size = Size(width * 0.52f, height * 0.28f),
        cornerRadius = CornerRadius(height * 0.14f, height * 0.14f)
    )
    drawLine(
        color = shoeColor,
        start = Offset(centerX - width * 0.24f, topY + height * 0.24f),
        end = Offset(centerX + width * 0.24f, topY + height * 0.24f),
        strokeWidth = height * 0.12f,
        cap = StrokeCap.Round
    )
    val dir = if (flip) -1f else 1f
    drawCircle(
        color = Color(0xFFF9A8D4),
        radius = height * 0.18f,
        center = Offset(centerX + dir * width * 0.12f, topY + height * 0.16f)
    )
}

private fun DrawScope.drawPinkMaidOutfit(
    cx: Float,
    torsoTop: Float,
    torsoWidth: Float,
    torsoHeight: Float,
    skirtBottomY: Float,
    maidPink: Color,
    maidPinkLight: Color,
    maidPinkDark: Color
) {
    val waistY = torsoTop + torsoHeight * 0.82f

    // Fitted pink maid bodice
    val bodicePath = Path().apply {
        moveTo(cx - torsoWidth * 0.44f, torsoTop)
        quadraticTo(cx, torsoTop + torsoHeight * 0.08f, cx + torsoWidth * 0.44f, torsoTop)
        lineTo(cx + torsoWidth * 0.40f, waistY)
        lineTo(cx - torsoWidth * 0.40f, waistY)
        close()
    }
    drawPath(path = bodicePath, color = maidPink)
    drawPath(
        path = bodicePath,
        color = maidPinkDark,
        style = Stroke(width = torsoWidth * 0.025f, join = StrokeJoin.Round)
    )

    // White bib & ruffled collar
    val bibPath = Path().apply {
        moveTo(cx - torsoWidth * 0.22f, torsoTop)
        lineTo(cx + torsoWidth * 0.22f, torsoTop)
        lineTo(cx + torsoWidth * 0.14f, waistY)
        lineTo(cx - torsoWidth * 0.14f, waistY)
        close()
    }
    drawPath(path = bibPath, color = Color.White)

    // Peter Pan ruffled white collar
    drawArc(
        color = Color.White,
        startAngle = 5f,
        sweepAngle = 170f,
        useCenter = true,
        topLeft = Offset(cx - torsoWidth * 0.26f, torsoTop - torsoHeight * 0.05f),
        size = Size(torsoWidth * 0.52f, torsoHeight * 0.22f)
    )

    // Flared pleated pink maid skirt
    val skirtLeft = cx - torsoWidth * 0.78f
    val skirtRight = cx + torsoWidth * 0.78f
    val skirtPath = Path().apply {
        moveTo(cx - torsoWidth * 0.40f, waistY)
        lineTo(cx + torsoWidth * 0.40f, waistY)
        quadraticTo(cx + torsoWidth * 0.72f, (waistY + skirtBottomY) * 0.5f, skirtRight, skirtBottomY)
        quadraticTo(cx, skirtBottomY + torsoHeight * 0.12f, skirtLeft, skirtBottomY)
        quadraticTo(cx - torsoWidth * 0.72f, (waistY + skirtBottomY) * 0.5f, cx - torsoWidth * 0.40f, waistY)
        close()
    }
    drawPath(
        path = skirtPath,
        brush = Brush.verticalGradient(
            colors = listOf(maidPink, maidPinkLight),
            startY = waistY,
            endY = skirtBottomY
        )
    )
    drawPath(
        path = skirtPath,
        color = maidPinkDark,
        style = Stroke(width = torsoWidth * 0.025f, join = StrokeJoin.Round)
    )

    // White scalloped lace trim at skirt bottom
    val laceCount = 7
    val span = skirtRight - skirtLeft
    val laceR = span / (laceCount * 2f)
    for (i in 0 until laceCount) {
        val lx = skirtLeft + laceR + i * (laceR * 2f)
        drawCircle(
            color = Color.White,
            radius = laceR * 1.05f,
            center = Offset(lx, skirtBottomY + laceR * 0.28f)
        )
    }

    // Crisp white maid apron over front of skirt
    val apronBottomY = skirtBottomY - (skirtBottomY - waistY) * 0.12f
    val apronPath = Path().apply {
        moveTo(cx - torsoWidth * 0.28f, waistY)
        lineTo(cx + torsoWidth * 0.28f, waistY)
        quadraticTo(cx + torsoWidth * 0.44f, (waistY + apronBottomY) * 0.55f, cx + torsoWidth * 0.46f, apronBottomY)
        quadraticTo(cx, apronBottomY + torsoHeight * 0.10f, cx - torsoWidth * 0.46f, apronBottomY)
        quadraticTo(cx - torsoWidth * 0.44f, (waistY + apronBottomY) * 0.55f, cx - torsoWidth * 0.28f, waistY)
        close()
    }
    drawPath(path = apronPath, color = Color.White)
    drawPath(
        path = apronPath,
        color = maidPinkLight,
        style = Stroke(width = torsoWidth * 0.02f, join = StrokeJoin.Round)
    )

    // White waist sash & cute pink bow on collar and waist
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(cx - torsoWidth * 0.42f, waistY - torsoHeight * 0.06f),
        size = Size(torsoWidth * 0.84f, torsoHeight * 0.11f),
        cornerRadius = CornerRadius(torsoWidth * 0.05f, torsoWidth * 0.05f)
    )
    drawMaidBow(
        center = Offset(cx, torsoTop + torsoHeight * 0.14f),
        width = torsoWidth * 0.26f,
        bowColor = maidPinkDark
    )
    drawMaidBow(
        center = Offset(cx, waistY),
        width = torsoWidth * 0.34f,
        bowColor = maidPinkDark
    )
}

private fun DrawScope.drawMaidBow(
    center: Offset,
    width: Float,
    bowColor: Color
) {
    val h = width * 0.45f
    val leftWing = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - width * 0.5f, center.y - h * 0.55f)
        quadraticTo(center.x - width * 0.62f, center.y, center.x - width * 0.5f, center.y + h * 0.55f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x + width * 0.5f, center.y - h * 0.55f)
        quadraticTo(center.x + width * 0.62f, center.y, center.x + width * 0.5f, center.y + h * 0.55f)
        close()
    }
    drawPath(path = leftWing, color = bowColor)
    drawPath(path = rightWing, color = bowColor)
    drawCircle(color = Color(0xFFF472B6), radius = h * 0.28f, center = center)
}

private fun DrawScope.drawHeartLens(
    center: Offset,
    radius: Float,
    tintColor: Color,
    frameColor: Color,
    frameStroke: Float
) {
    val cx = center.x
    val cy = center.y + radius * 0.08f
    val heartPath = Path().apply {
        moveTo(cx, cy - radius * 0.45f)
        cubicTo(
            cx - radius * 0.35f, cy - radius * 1.15f,
            cx - radius * 1.25f, cy - radius * 0.65f,
            cx - radius * 1.05f, cy + radius * 0.08f
        )
        cubicTo(
            cx - radius * 0.85f, cy + radius * 0.68f,
            cx - radius * 0.25f, cy + radius * 0.92f,
            cx, cy + radius * 1.10f
        )
        cubicTo(
            cx + radius * 0.25f, cy + radius * 0.92f,
            cx + radius * 0.85f, cy + radius * 0.68f,
            cx + radius * 1.05f, cy + radius * 0.08f
        )
        cubicTo(
            cx + radius * 1.25f, cy - radius * 0.65f,
            cx + radius * 0.35f, cy - radius * 1.15f,
            cx, cy - radius * 0.45f
        )
        close()
    }
    drawPath(path = heartPath, color = tintColor)
    drawLine(
        color = Color(0x99FFFFFF),
        start = Offset(cx - radius * 0.56f, cy - radius * 0.42f),
        end = Offset(cx - radius * 0.26f, cy - radius * 0.62f),
        strokeWidth = frameStroke * 0.6f,
        cap = StrokeCap.Round
    )
    drawPath(
        path = heartPath,
        color = frameColor,
        style = Stroke(width = frameStroke, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawCatEarsHeadband(
    cx: Float,
    headCenterY: Float,
    headRadiusX: Float,
    headRadiusY: Float,
    outerPink: Color,
    darkPink: Color
) {
    // Pink headband curved over top of hair
    drawArc(
        color = darkPink,
        startAngle = 195f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(cx - headRadiusX * 0.96f, headCenterY - headRadiusY * 1.30f),
        size = Size(headRadiusX * 1.92f, headRadiusY * 1.10f),
        style = Stroke(width = headRadiusX * 0.085f, cap = StrokeCap.Round)
    )

    // Left & Right pointed cat ears
    for (dir in listOf(-1f, 1f)) {
        val baseInnerX = cx + dir * headRadiusX * 0.26f
        val baseOuterX = cx + dir * headRadiusX * 0.84f
        val baseY = headCenterY - headRadiusY * 1.04f
        val tipX = cx + dir * headRadiusX * 0.72f
        val tipY = headCenterY - headRadiusY * 1.78f

        val outerEar = Path().apply {
            moveTo(baseInnerX, baseY - headRadiusY * 0.14f)
            quadraticTo(
                cx + dir * headRadiusX * 0.45f,
                tipY + headRadiusY * 0.18f,
                tipX,
                tipY
            )
            quadraticTo(
                cx + dir * headRadiusX * 0.90f,
                tipY + headRadiusY * 0.35f,
                baseOuterX,
                baseY + headRadiusY * 0.12f
            )
            close()
        }
        drawPath(path = outerEar, color = outerPink)
        drawPath(
            path = outerEar,
            color = darkPink,
            style = Stroke(width = headRadiusX * 0.045f, join = StrokeJoin.Round)
        )

        val innerEar = Path().apply {
            moveTo(baseInnerX + dir * headRadiusX * 0.10f, baseY - headRadiusY * 0.04f)
            lineTo(tipX - dir * headRadiusX * 0.04f, tipY + headRadiusY * 0.22f)
            lineTo(baseOuterX - dir * headRadiusX * 0.10f, baseY + headRadiusY * 0.10f)
            close()
        }
        drawPath(path = innerEar, color = Color(0xFFFCE7F3))
    }
}

