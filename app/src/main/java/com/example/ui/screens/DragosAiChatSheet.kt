package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.PetUiState
import com.example.ui.theme.CobaltPrimary

@Composable
fun DragosAiChatSheet(
    state: PetUiState,
    onClose: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isFemboy = state.femboyMode
    val accentPrimary = if (isFemboy) Color(0xFFEC4899) else CobaltPrimary
    val accentSecondary = if (isFemboy) Color(0xFFF472B6) else Color(0xFF0EA5E9)
    val sheetBg = if (isFemboy) Color(0xFFFFF1F7) else Color(0xFFF8FAFC)
    val userBubbleGradient = Brush.linearGradient(listOf(accentPrimary, accentSecondary))

    val quickPrompts = if (isFemboy) {
        listOf(
            "ai_chip_good_boy" to "You're a good boy! :3",
            "ai_chip_meow" to "Can you meow for me?",
            "ai_chip_outfit" to "Do you like your pink maid outfit?"
        )
    } else {
        listOf(
            "ai_chip_good_boy" to "You're a good boy!",
            "ai_chip_food" to "Want some spinach or pizza?",
            "ai_chip_alex" to "What are you and Alex up to?"
        )
    }

    LaunchedEffect(state.chatMessages.size, state.streamingAiText, state.isAiLoading) {
        val total = state.chatMessages.size + if (state.isAiLoading || !state.streamingAiText.isNullOrEmpty()) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x880F172A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = sheetBg,
            shadowElevation = 18.dp,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .imePadding()
                .navigationBarsPadding()
                .testTag("ai_chat_sheet")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isFemboy) {
                                    listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8))
                                } else {
                                    listOf(Color(0xFFE0F2FE), Color(0xFFDBEAFE))
                                }
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(userBubbleGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFemboy) "🐱" else "🤓",
                            fontSize = 22.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFemboy) "Dragoș :3 (Femboy Mode)" else "Chat with Dragoș",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isFemboy) Color(0xFF9D174D) else Color(0xFF0F172A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Powered by Gemini",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFemboy) Color(0xFFBE185D) else Color(0xFF334155)
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_ai_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close AI chat",
                            tint = Color(0xFF334155)
                        )
                    }
                }

                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.chatMessages.isEmpty() && !state.isAiLoading) {
                        item(key = "welcome_card") {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, accentPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (isFemboy) {
                                            "Nya~ hii!! *twirls pink maid skirt* Ask me anything or call me a good boy~ :3 💖"
                                        } else {
                                            "Salut! I'm Dragoș! Ask me about my OnePlus 13, soccer, sarmale, or anything else!"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }

                    items(state.chatMessages, key = { it.id }) { msg ->
                        ChatBubbleItem(
                            message = msg,
                            userGradient = userBubbleGradient,
                            accentColor = accentPrimary
                        )
                    }

                    if (state.isAiLoading) {
                        item(key = "streaming_bubble") {
                            Surface(
                                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .widthIn(max = 310.dp)
                                    .border(1.dp, accentPrimary.copy(alpha = 0.22f), RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                            ) {
                                val streaming = state.streamingAiText
                                if (streaming.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = accentPrimary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = if (isFemboy) "Dragoș is typing~ :3" else "Dragoș is thinking...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = streaming,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Banner (surfaces real Firebase AI / App Check errors)
                if (state.aiError != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .testTag("ai_chat_error")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.aiError,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ai_chat_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Retry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Quick Prompts Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPrompts, key = { it.first }) { (tag, promptText) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isFemboy) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !state.isAiLoading) {
                                    onSendMessage(promptText)
                                }
                                .testTag(tag)
                        ) {
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isFemboy) Color(0xFFBE185D) else Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isFemboy) "Say something sweet~ :3" else "Message Dragoș...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !state.isAiLoading) {
                                    val msg = inputText
                                    inputText = ""
                                    onSendMessage(msg)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentPrimary,
                            unfocusedBorderColor = accentPrimary.copy(alpha = 0.35f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_chat_input")
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = inputText.isNotBlank() && !state.isAiLoading
                    IconButton(
                        onClick = {
                            val msg = inputText
                            inputText = ""
                            onSendMessage(msg)
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (canSend) accentPrimary else Color(0xFFCBD5E1))
                            .testTag("ai_chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    userGradient: Brush,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.isFromUser) {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(userGradient)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 305.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
