package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DragosReaction
import com.example.ui.character.DragosCharacter
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralRose
import com.example.ui.theme.MintGreen
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class FallingKind { SOCCER, STAR, COIN }

private data class FallingTarget(
    val id: Long,
    var xFrac: Float,
    var yFrac: Float,
    var speedY: Float,
    val kind: FallingKind
)

@Composable
fun MiniGameOverlay(
    highScore: Int,
    femboyMode: Boolean = false,
    onFinish: (score: Int, coinsEarned: Int) -> Unit,
    onPlayKickSound: () -> Unit,
    onPlayCoinSound: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var coinsEarned by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var gameOver by remember { mutableStateOf(false) }
    var dragosXFrac by remember { mutableFloatStateOf(0.5f) }
    var kickReaction by remember { mutableStateOf(DragosReaction.IDLE) }

    val targets = remember { mutableStateListOf<FallingTarget>() }
    var nextId by remember { mutableIntStateOf(1) }

    LaunchedEffect(gameOver) {
        if (gameOver) return@LaunchedEffect
        var lastNanos = 0L
        var spawnTimer = 0.6f
        while (!gameOver) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos

                spawnTimer -= dt
                if (spawnTimer <= 0f) {
                    val roll = Random.nextFloat()
                    val kind = when {
                        roll < 0.18f -> FallingKind.COIN
                        roll < 0.36f -> FallingKind.STAR
                        else -> FallingKind.SOCCER
                    }
                    val difficultyBoost = (score / 120f).coerceAtMost(0.28f)
                    targets.add(
                        FallingTarget(
                            id = nextId++.toLong(),
                            xFrac = Random.nextFloat() * 0.76f + 0.12f,
                            yFrac = -0.05f,
                            speedY = 0.28f + Random.nextFloat() * 0.12f + difficultyBoost,
                            kind = kind
                        )
                    )
                    spawnTimer = (0.95f - (score / 250f)).coerceAtLeast(0.42f)
                }

                val iter = targets.listIterator()
                while (iter.hasNext()) {
                    val item = iter.next()
                    item.yFrac += item.speedY * dt

                    // Auto-catch / kick by Dragoș at bottom zone!
                    if (item.yFrac in 0.76f..0.88f && abs(item.xFrac - dragosXFrac) < 0.15f) {
                        iter.remove()
                        when (item.kind) {
                            FallingKind.SOCCER -> {
                                score += 10
                                coinsEarned += 1
                                kickReaction = DragosReaction.SOCCER_KICK
                                onPlayKickSound()
                            }
                            FallingKind.STAR -> {
                                score += 20
                                coinsEarned += 2
                                kickReaction = DragosReaction.HAPPY_LAUGH
                                onPlayCoinSound()
                            }
                            FallingKind.COIN -> {
                                score += 15
                                coinsEarned += 4
                                kickReaction = DragosReaction.JUMPING
                                onPlayCoinSound()
                            }
                        }
                    } else if (item.yFrac > 0.96f) {
                        iter.remove()
                        if (item.kind == FallingKind.SOCCER) {
                            lives -= 1
                            if (lives <= 0) {
                                gameOver = true
                            }
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0284C7))
                )
            )
            .pointerInput(gameOver) {
                if (!gameOver) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        dragosXFrac = (dragosXFrac + dragAmount.x / size.width.coerceAtLeast(1))
                            .coerceIn(0.12f, 0.88f)
                    }
                }
            }
            .pointerInput(gameOver) {
                if (!gameOver) {
                    detectTapGestures { offset ->
                        val tapX = offset.x / size.width.coerceAtLeast(1)
                        val tapY = offset.y / size.height.coerceAtLeast(1)
                        val hit = targets.firstOrNull {
                            abs(it.xFrac - tapX) < 0.11f && abs(it.yFrac - tapY) < 0.09f
                        }
                        if (hit != null) {
                            targets.remove(hit)
                            score += if (hit.kind == FallingKind.STAR) 20 else 12
                            coinsEarned += if (hit.kind == FallingKind.COIN) 4 else 1
                            kickReaction = DragosReaction.SOCCER_KICK
                            onPlayKickSound()
                        } else {
                            dragosXFrac = tapX.coerceIn(0.12f, 0.88f)
                        }
                    }
                }
            }
    ) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }
        val hPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Subtle pitch lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x1FFFFFFF),
                radius = size.minDimension * 0.25f,
                center = Offset(size.width * 0.5f, size.height * 0.45f)
            )
            drawLine(
                color = Color(0x28FFFFFF),
                start = Offset(0f, size.height * 0.86f),
                end = Offset(size.width, size.height * 0.86f),
                strokeWidth = 4.dp.toPx()
            )
        }

        // Top Game Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33FFFFFF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score: $score",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "🪙 +$coinsEarned",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberGold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { idx ->
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (idx < lives) CoralRose else Color(0x44FFFFFF),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x33FFFFFF)
                ) {
                    IconButton(
                        onClick = { onFinish(score, coinsEarned) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_minigame_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit mini-game",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Falling targets
        targets.forEach { target ->
            val emoji = when (target.kind) {
                FallingKind.SOCCER -> "⚽"
                FallingKind.STAR -> "⭐"
                FallingKind.COIN -> "🪙"
            }
            Text(
                text = emoji,
                fontSize = 36.sp,
                modifier = Modifier.offset {
                    IntOffset(
                        x = (target.xFrac * wPx - 22.dp.toPx()).roundToInt(),
                        y = (target.yFrac * hPx - 22.dp.toPx()).roundToInt()
                    )
                }
            )
        }

        // Controllable Dragoș at bottom
        Box(
            modifier = Modifier
                .size(160.dp, 210.dp)
                .offset {
                    IntOffset(
                        x = (dragosXFrac * wPx - 80.dp.toPx()).roundToInt(),
                        y = (hPx * 0.71f).roundToInt()
                    )
                }
        ) {
            DragosCharacter(
                reaction = kickReaction,
                mouthAmplitude = 0f,
                femboyMode = femboyMode
            )
        }

        // Hint pill at top-center
        if (!gameOver && score == 0) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0x440F172A),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp)
            ) {
                Text(
                    text = "Drag Dragoș or tap balls to juggle!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Game Over Summary Card
        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990F172A)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "⚽", fontSize = 44.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Great Match!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Best Score: ${maxOf(highScore, score)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$score", style = MaterialTheme.typography.displaySmall, color = CobaltPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("COINS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+$coinsEarned", style = MaterialTheme.typography.displaySmall, color = MintGreen)
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    score = 0
                                    coinsEarned = 0
                                    lives = 3
                                    targets.clear()
                                    gameOver = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Filled.Replay, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Again")
                            }
                            Button(
                                onClick = { onFinish(score, coinsEarned) },
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("minigame_collect_button")
                            ) {
                                Text("Collect")
                            }
                        }
                    }
                }
            }
        }
    }
}
