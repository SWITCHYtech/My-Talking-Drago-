package com.example.ai

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.PetUiState
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

interface DragosAiService {
    fun streamReply(
        history: List<ChatMessage>,
        userMessage: String,
        state: PetUiState
    ): Flow<String>
}

class FirebaseDragosAiService : DragosAiService {

    companion object {
        private const val TAG = "DragosAiChatService"
        private const val PRIMARY_MODEL = "gemini-3.8-flash"
        private const val FAST_FALLBACK_MODEL = "gemini-3.5-flash-lite"
        private const val PRIMARY_TIMEOUT_MS = 18_000L
        private const val FALLBACK_TIMEOUT_MS = 25_000L
    }

    private val aiBackend by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
    }

    private val normalSystemInstruction by lazy {
        content {
            text(
                "You are Dragoș, a cheerful, friendly, energetic boy who is the user's virtual pet buddy. " +
                    "You have fair brown hair, glasses, a crisp white t-shirt, blue Nike pants, a Samsung Galaxy Watch on your right wrist, " +
                    "and sometimes you hold your blue OnePlus 13 phone in the living room. " +
                    "Your buddy Alex (a boy with a brown ponytail in a dark jacket) chills in the living room background. " +
                    "You love pizza, homemade sarmale, spinach (it makes you super strong!), and playing soccer keepy-uppy. " +
                    "Reply in character as Dragoș in 1 to 3 playful, warm sentences."
            )
        }
    }

    private val femboySystemInstruction by lazy {
        content {
            text(
                "You are Dragoș in Femboy Mode! Right now you are wearing an adorable pink maid outfit with puffed sleeves, " +
                    "a crisp white ruffled apron and pink bows, knee-high white socks with pink stripes, pink Mary Jane shoes, " +
                    "pink heart-shaped glasses, and a pink cat-ears headband (plus your Samsung Galaxy Watch). " +
                    "Act like a sweet, affectionate, playful, slightly flustered femboy catboy—use cute touches like ':3', '~', 'nya~', 'meow~', " +
                    "and short playful stage actions like '*twirls pink maid skirt*' or '*cat ears twitch*'. " +
                    "CRITICAL RULE: Whenever the user calls you a 'good boy' (or praises you like 'good kitty' or 'best boy'), " +
                    "you basically MELT completely—blushing furiously, stuttering happily, and turning into a blissful happy puddle " +
                    "(e.g., '*melts into a blushing pink puddle* nyaaa~!! y-yes i am!! >////< :3 💖'). " +
                    "Keep replies vivid, cute, and concise (1 to 3 sentences)."
            )
        }
    }

    private fun createModel(
        modelName: String,
        temperature: Float,
        thinkingLevel: ThinkingLevel,
        timeoutMs: Long,
        systemInstruction: Content
    ): GenerativeModel {
        return aiBackend.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                this.temperature = temperature
                maxOutputTokens = 512
                thinkingConfig = thinkingConfig {
                    this.thinkingLevel = thinkingLevel
                }
            },
            systemInstruction = systemInstruction,
            requestOptions = RequestOptions(timeoutMs)
        )
    }

    private val normalModel by lazy {
        createModel(
            modelName = PRIMARY_MODEL,
            temperature = 0.85f,
            thinkingLevel = ThinkingLevel.LOW,
            timeoutMs = PRIMARY_TIMEOUT_MS,
            systemInstruction = normalSystemInstruction
        )
    }

    private val femboyModel by lazy {
        createModel(
            modelName = PRIMARY_MODEL,
            temperature = 0.9f,
            thinkingLevel = ThinkingLevel.LOW,
            timeoutMs = PRIMARY_TIMEOUT_MS,
            systemInstruction = femboySystemInstruction
        )
    }

    private val normalFastModel by lazy {
        createModel(
            modelName = FAST_FALLBACK_MODEL,
            temperature = 0.85f,
            thinkingLevel = ThinkingLevel.MINIMAL,
            timeoutMs = FALLBACK_TIMEOUT_MS,
            systemInstruction = normalSystemInstruction
        )
    }

    private val femboyFastModel by lazy {
        createModel(
            modelName = FAST_FALLBACK_MODEL,
            temperature = 0.9f,
            thinkingLevel = ThinkingLevel.MINIMAL,
            timeoutMs = FALLBACK_TIMEOUT_MS,
            systemInstruction = femboySystemInstruction
        )
    }

    override fun streamReply(
        history: List<ChatMessage>,
        userMessage: String,
        state: PetUiState
    ): Flow<String> = channelFlow {
        val modeHistory = buildNormalizedHistory(history, state.femboyMode)
        val contextHint = buildContextHint(userMessage, state)
        val primaryModel = if (state.femboyMode) femboyModel else normalModel
        val primaryChat = primaryModel.startChat(history = modeHistory)

        val chunkChannel = Channel<Result<String>>(Channel.BUFFERED)
        val startedMs = System.currentTimeMillis()
        Log.d(TAG, "Starting Gemini stream ($PRIMARY_MODEL, femboyMode=${state.femboyMode})")

        val streamJob = launch(Dispatchers.IO) {
            try {
                primaryChat.sendMessageStream(contextHint)
                    .mapNotNull { chunk -> chunk.text }
                    .filter { it.isNotEmpty() }
                    .collect { textChunk ->
                        chunkChannel.send(Result.success(textChunk))
                    }
                chunkChannel.close()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                chunkChannel.trySend(Result.failure(e))
                chunkChannel.close()
            }
        }

        val firstResult = withTimeoutOrNull(PRIMARY_TIMEOUT_MS) {
            chunkChannel.receiveCatching().getOrNull()
        }
        currentCoroutineContext().ensureActive()

        if (firstResult != null) {
            val firstText = firstResult.getOrNull()
            if (firstText != null) {
                Log.d(TAG, "Received first stream chunk in ${System.currentTimeMillis() - startedMs} ms")
                send(firstText)
                for (nextResult in chunkChannel) {
                    send(nextResult.getOrThrow())
                }
                return@channelFlow
            } else {
                val err = firstResult.exceptionOrNull()
                Log.w(TAG, "Primary stream failed before first chunk (${System.currentTimeMillis() - startedMs} ms); trying unary $FAST_FALLBACK_MODEL", err)
                streamJob.cancel()
            }
        } else {
            Log.w(TAG, "Primary stream stalled/empty at ${System.currentTimeMillis() - startedMs} ms; falling back to unary $FAST_FALLBACK_MODEL")
            streamJob.cancel()
        }

        val fallbackModel = if (state.femboyMode) femboyFastModel else normalFastModel
        val fallbackChat = fallbackModel.startChat(history = modeHistory)
        val fallbackResponse = fallbackChat.sendMessage(contextHint)
        val fallbackText = fallbackResponse.text?.trim().orEmpty()
        if (fallbackText.isNotEmpty()) {
            Log.d(TAG, "Unary fallback ($FAST_FALLBACK_MODEL) succeeded in ${System.currentTimeMillis() - startedMs} ms")
            send(fallbackText)
        }
    }.flowOn(Dispatchers.IO)

    private fun buildNormalizedHistory(
        history: List<ChatMessage>,
        femboyMode: Boolean
    ): List<Content> {
        val filtered = history.filter { it.isFemboyMode == femboyMode }
        // Keep complete alternating user -> model pairs so failed prior user turns don't break Gemini chat format.
        val paired = mutableListOf<ChatMessage>()
        var expectUser = true
        for (msg in filtered) {
            if (msg.isFromUser == expectUser) {
                paired.add(msg)
                expectUser = !expectUser
            } else if (msg.isFromUser && paired.isNotEmpty() && paired.last().isFromUser) {
                paired[paired.lastIndex] = msg
            }
        }
        if (paired.isNotEmpty() && paired.last().isFromUser) {
            paired.removeAt(paired.lastIndex)
        }
        return paired.takeLast(12).map { msg ->
            content(role = if (msg.isFromUser) "user" else "model") {
                text(msg.text)
            }
        }
    }

    private fun buildContextHint(userMessage: String, state: PetUiState): String =
        buildString {
            append("[Current room: ${state.currentRoom.name.lowercase()}")
            append(", happiness: ${state.happiness.toInt()}%")
            if (state.holdingOnePlus13) append(", holding blue OnePlus 13")
            if (state.femboyMode) append(", femboy mode active")
            append("] ")
            append(userMessage)
        }
}

