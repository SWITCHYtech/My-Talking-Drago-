package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.PetSoundEffect
import com.example.model.DragosReaction
import com.example.model.FoodCatalog
import com.example.model.FoodItem
import com.example.model.PetRoom
import com.example.model.PetUiState
import com.example.ui.character.AlexBackgroundCharacter
import com.example.ui.character.DragosCharacter
import com.example.ui.components.BottomRoomBar
import com.example.ui.components.FloatingParticlesLayer
import com.example.ui.components.SpeechBubbleBanner
import com.example.ui.components.TopPetHud
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralRose
import com.example.ui.theme.MintGreen
import com.example.ui.theme.SkyAccent
import com.example.viewmodel.DragosViewModel

@Composable
fun DragosHomeScreen(
    viewModel: DragosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var micDeniedMessage by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micDeniedMessage = false
            viewModel.toggleVoiceRecording()
        } else {
            micDeniedMessage = true
        }
    }

    val handleMicClick = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            micDeniedMessage = false
            viewModel.toggleVoiceRecording()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (state.isMiniGameActive) {
        BackHandler {
            viewModel.finishMiniGame(score = 0, coinsEarned = 0)
        }
        MiniGameOverlay(
            highScore = state.highScoreKeepyUppy,
            femboyMode = state.femboyMode,
            onFinish = { score, coins -> viewModel.finishMiniGame(score, coins) },
            onPlayKickSound = { viewModel.soundEngine.playEffect(PetSoundEffect.KICK) },
            onPlayCoinSound = { viewModel.soundEngine.playEffect(PetSoundEffect.COIN) }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Room Backdrop with Crossfade
        Crossfade(
            targetState = state.currentRoom,
            animationSpec = tween(350),
            label = "room_backdrop"
        ) { room ->
            val drawableRes = when (room) {
                PetRoom.LIVING -> R.drawable.img_room_living
                PetRoom.KITCHEN -> R.drawable.img_room_kitchen
                PetRoom.PLAYROOM -> R.drawable.img_room_playroom
                PetRoom.BEDROOM -> R.drawable.img_room_bedroom
            }
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "${room.name} room backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle dark night tint when Bedroom lights are off
        AnimatedVisibility(
            visible = state.currentRoom == PetRoom.BEDROOM && state.lightsOff,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xC2090D1F), Color(0xA60F172A))
                        )
                    )
            )
        }

        // Main Responsive Stage
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopPetHud(
                state = state,
                onToggleSound = { viewModel.toggleSound() }
            )

            // Character & Speech Stage
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val stageHeight = maxHeight
                val charHeight = (stageHeight * 0.84f).coerceIn(140.dp, 460.dp)
                val charWidth = charHeight * 0.66f

                // Speech bubble pinned above Dragoș
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bubbleText = when {
                        micDeniedMessage -> stringResource(R.string.mic_permission_needed)
                        else -> state.speechBubble
                    }
                    SpeechBubbleBanner(text = bubbleText)

                    if (micDeniedMessage) {
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text(stringResource(R.string.allow_mic))
                        }
                    }
                }

                // Alex hanging out in the background of the Living Room
                if (state.currentRoom == PetRoom.LIVING) {
                    val alexHeight = (stageHeight * 0.48f).coerceIn(96.dp, 220.dp)
                    val alexWidth = alexHeight * 0.64f
                    val alexBottomPad = (stageHeight * 0.14f).coerceIn(18.dp, 58.dp)
                    AlexBackgroundCharacter(
                        isWaving = state.alexWaving || state.reaction == DragosReaction.GOOD_BOY,
                        onTap = { viewModel.onAlexTapped() },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = alexBottomPad)
                            .size(width = alexWidth, height = alexHeight)
                    )
                }

                // Dragoș Character
                DragosCharacter(
                    reaction = state.reaction,
                    mouthAmplitude = state.mouthAmplitude,
                    holdingOnePlus13 = state.holdingOnePlus13,
                    femboyMode = state.femboyMode,
                    onBodyTap = { zone -> viewModel.onBodyTap(zone) },
                    onHeadPetting = { viewModel.onHeadPetting() },
                    modifier = Modifier
                        .size(width = charWidth, height = charHeight)
                        .padding(bottom = 6.dp)
                )

                // Flying Food Animation when feeding
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.activeFoodEmoji != null,
                    enter = fadeIn(tween(120)) + scaleIn(initialScale = 1.6f),
                    exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.25f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-32).dp)
                ) {
                    Text(
                        text = state.activeFoodEmoji.orEmpty(),
                        fontSize = 56.sp
                    )
                }

                // Floating Reaction Particles
                FloatingParticlesLayer(particles = state.particles)

                // Prominent Gemini AI Chat Button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,
                    color = Color.Transparent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (state.femboyMode) {
                                    listOf(Color(0xFFEC4899), Color(0xFFDB2777))
                                } else {
                                    listOf(CobaltPrimary, Color(0xFF0EA5E9))
                                }
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                        .clickable { viewModel.openAiChat() }
                        .testTag("ai_chat_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Chat with Dragoș AI",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Room-specific Action Deck
            AnimatedContent(
                targetState = state.currentRoom,
                label = "room_controls"
            ) { room ->
                when (room) {
                    PetRoom.LIVING -> LivingRoomControls(
                        state = state,
                        onMicClick = handleMicClick,
                        onAction = { viewModel.triggerAction(it) }
                    )
                    PetRoom.KITCHEN -> KitchenControls(
                        coins = state.coins,
                        onFeed = { viewModel.feedDragos(it) }
                    )
                    PetRoom.PLAYROOM -> PlayroomControls(
                        highScore = state.highScoreKeepyUppy,
                        onStartMiniGame = { viewModel.startMiniGame() },
                        onAction = { viewModel.triggerAction(it) }
                    )
                    PetRoom.BEDROOM -> BedroomControls(
                        lightsOff = state.lightsOff,
                        onToggleLights = { viewModel.toggleLights() },
                        onWashUp = { viewModel.washUp() }
                    )
                }
            }

            BottomRoomBar(
                currentRoom = state.currentRoom,
                onSelectRoom = { viewModel.selectRoom(it) }
            )
        }

        if (state.showFemboyPopup) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissFemboyPopup() },
                modifier = Modifier.testTag("femboy_mode_popup"),
                containerColor = Color(0xFFFDF2F8),
                title = {
                    Text(
                        text = "femboy mode activated",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFDB2777)
                    )
                },
                text = {
                    Text(
                        text = "🎀 Pink maid outfit, pink heart glasses & cat ears unlocked! Try calling Dragoș a Good Boy or touching him~ :3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF831843)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissFemboyPopup() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dismiss_femboy_popup_button")
                    ) {
                        Text(text = "Meow! :3", color = Color.White)
                    }
                }
            )
        }

        if (state.isAiChatOpen) {
            DragosAiChatSheet(
                state = state,
                onClose = { viewModel.closeAiChat() },
                onSendMessage = { viewModel.sendAiChatMessage(it) },
                onRetry = { viewModel.retryLastAiMessage() }
            )
        }
    }
}

@Composable
private fun LivingRoomControls(
    state: PetUiState,
    onMicClick: () -> Unit,
    onAction: (DragosReaction) -> Unit
) {
    val micScale by animateFloatAsState(
        targetValue = if (state.isListening) 1.08f + state.micAmplitude * 0.25f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "mic_pulse"
    )

    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionButton(
                icon = Icons.Filled.WavingHand,
                label = "Wave",
                tint = SkyAccent,
                testTag = "action_wave_button",
                onClick = { onAction(DragosReaction.WAVE) }
            )

            QuickActionButton(
                icon = Icons.Filled.Favorite,
                label = "Good Boy",
                tint = CoralRose,
                testTag = "action_good_boy_button",
                onClick = { onAction(DragosReaction.GOOD_BOY) }
            )

            // Central Talking Tom Mic Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .scale(micScale)
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                state.isListening -> CoralRose
                                state.isTalking -> MintGreen
                                else -> CobaltPrimary
                            }
                        )
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { onMicClick() }
                        .testTag("talk_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isListening || state.isTalking) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (state.isListening) "Stop recording" else "Talk to Dragoș",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xCC0F172A)
                ) {
                    Text(
                        text = when {
                            state.isListening -> stringResource(R.string.listening_prompt)
                            state.isTalking -> stringResource(R.string.repeating_prompt)
                            else -> "Talk"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            QuickActionButton(
                icon = Icons.Filled.MusicNote,
                label = "Dance",
                tint = BerryPurple,
                testTag = "action_dance_button",
                onClick = { onAction(DragosReaction.DANCING) }
            )

            QuickActionButton(
                icon = Icons.Filled.DirectionsRun,
                label = "Jump",
                tint = AmberGold,
                testTag = "action_jump_button",
                onClick = { onAction(DragosReaction.JUMPING) }
            )
        }
    }
}

@Composable
private fun KitchenControls(
    coins: Int,
    onFeed: (FoodItem) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("kitchen_food_tray"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(FoodCatalog.items, key = { it.id }) { food ->
            val canAfford = coins >= food.cost
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (canAfford) 0.94f else 0.65f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .width(92.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onFeed(food) }
                    .testTag("food_item_${food.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = food.emoji, fontSize = 30.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (food.cost == 0) "Free" else "🪙 ${food.cost}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (food.cost == 0) MintGreen else AmberGold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayroomControls(
    highScore: Int,
    onStartMiniGame: () -> Unit,
    onAction: (DragosReaction) -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onStartMiniGame,
            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .testTag("start_minigame_button")
        ) {
            Icon(Icons.Filled.SportsSoccer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.play_minigame),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Best: $highScore",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        QuickActionButton(
            icon = Icons.Filled.SportsSoccer,
            label = "Kick",
            tint = MintGreen,
            testTag = "action_kick_button",
            onClick = { onAction(DragosReaction.SOCCER_KICK) }
        )

        QuickActionButton(
            icon = Icons.Filled.MusicNote,
            label = "Dance",
            tint = BerryPurple,
            testTag = "playroom_dance_button",
            onClick = { onAction(DragosReaction.DANCING) }
        )
    }
}

@Composable
private fun BedroomControls(
    lightsOff: Boolean,
    onToggleLights: () -> Unit,
    onWashUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onToggleLights,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (lightsOff) AmberGold else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .testTag("toggle_lights_button")
        ) {
            Icon(
                imageVector = if (lightsOff) Icons.Filled.Lightbulb else Icons.Filled.NightlightRound,
                contentDescription = null,
                tint = if (lightsOff) Color(0xFF0F172A) else Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (lightsOff) stringResource(R.string.lights_on) else stringResource(R.string.lights_off),
                color = if (lightsOff) Color(0xFF0F172A) else Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Button(
            onClick = onWashUp,
            colors = ButtonDefaults.buttonColors(containerColor = SkyAccent),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .testTag("wash_up_button")
        ) {
            Icon(Icons.Filled.CleaningServices, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.wash_up),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
