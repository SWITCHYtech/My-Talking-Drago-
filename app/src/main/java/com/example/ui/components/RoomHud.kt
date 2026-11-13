package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FloatingParticle
import com.example.model.PetRoom
import com.example.model.PetUiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralRose
import com.example.ui.theme.MintGreen
import com.example.ui.theme.SkyAccent
import kotlin.math.roundToInt

@Composable
fun TopPetHud(
    state: PetUiState,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level & XP Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CobaltPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.level}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dragoș",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.xp % 100}/100 XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins Chip
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.testTag("coin_counter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${state.coins}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Sound Toggle Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (state.soundEnabled) "Mute sound effects" else "Unmute sound effects",
                            tint = if (state.soundEnabled) CobaltPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4 Circular Stat Meters
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatRing(
                    label = stringResource(R.string.stat_happiness),
                    value = state.happiness,
                    icon = Icons.Filled.Favorite,
                    baseColor = CoralRose
                )
                StatRing(
                    label = stringResource(R.string.stat_hunger),
                    value = state.hunger,
                    icon = Icons.Filled.Restaurant,
                    baseColor = AmberGold
                )
                StatRing(
                    label = stringResource(R.string.stat_energy),
                    value = state.energy,
                    icon = Icons.Filled.Bolt,
                    baseColor = MintGreen
                )
                StatRing(
                    label = stringResource(R.string.stat_freshness),
                    value = state.freshness,
                    icon = Icons.Filled.WaterDrop,
                    baseColor = SkyAccent
                )
            }
        }
    }
}

@Composable
private fun StatRing(
    label: String,
    value: Float,
    icon: ImageVector,
    baseColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (value / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 220f),
        label = "ring_$label"
    )
    val ringColor = when {
        value < 25f -> CoralRose
        value < 50f -> AmberGold
        else -> baseColor
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.5.dp.toPx()
                drawArc(
                    color = ringColor.copy(alpha = 0.18f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = "$label ${value.roundToInt()} percent",
                tint = ringColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpeechBubbleBanner(
    text: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !text.isNullOrBlank(),
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.86f),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.90f),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .border(2.dp, CobaltPrimary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
        ) {
            Text(
                text = text.orEmpty(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun BottomRoomBar(
    currentRoom: PetRoom,
    onSelectRoom: (PetRoom) -> Unit,
    modifier: Modifier = Modifier
) {
    val rooms = listOf(
        Triple(PetRoom.LIVING, stringResource(R.string.room_living), Icons.Filled.Home to Icons.Outlined.Home),
        Triple(PetRoom.KITCHEN, stringResource(R.string.room_kitchen), Icons.Filled.Restaurant to Icons.Outlined.Restaurant),
        Triple(PetRoom.PLAYROOM, stringResource(R.string.room_playroom), Icons.Filled.SportsSoccer to Icons.Outlined.SportsSoccer),
        Triple(PetRoom.BEDROOM, stringResource(R.string.room_bedroom), Icons.Filled.Bedtime to Icons.Outlined.Bedtime)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rooms.forEach { (room, label, icons) ->
                    val selected = room == currentRoom
                    val bgColor = if (selected) CobaltPrimary else Color.Transparent
                    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(22.dp))
                            .background(bgColor)
                            .clickable { onSelectRoom(room) }
                            .testTag("room_tab_${room.name.lowercase()}")
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selected) icons.first else icons.second,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingParticlesLayer(
    particles: List<FloatingParticle>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            var started by remember(particle.id) { mutableStateOf(false) }
            LaunchedEffect(particle.id) { started = true }

            val yShift by animateFloatAsState(
                targetValue = if (started) -130f else 0f,
                animationSpec = tween(durationMillis = 1150),
                label = "p_y_${particle.id}"
            )
            val alpha by animateFloatAsState(
                targetValue = if (started) 0f else 1f,
                animationSpec = tween(durationMillis = 1150),
                label = "p_a_${particle.id}"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = particle.emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset {
                            IntOffset(
                                x = ((particle.xFraction - 0.5f) * 420f).roundToInt(),
                                y = yShift.roundToInt() - 60
                            )
                        }
                        .padding(4.dp),
                    color = Color.Unspecified.copy(alpha = alpha)
                )
            }
        }
    }
}
