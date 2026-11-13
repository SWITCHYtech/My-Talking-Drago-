package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.DragosAiService
import com.example.ai.FirebaseDragosAiService
import com.example.audio.PetSoundEffect
import com.example.audio.PetSoundEngine
import com.example.data.PersistedPetData
import com.example.data.PetPreferencesRepository
import com.example.model.BodyZone
import com.example.model.ChatMessage
import com.example.model.DragosReaction
import com.example.model.FloatingParticle
import com.example.model.FoodItem
import com.example.model.PetRoom
import com.example.model.PetUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

class DragosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetPreferencesRepository(application.applicationContext)
    val soundEngine = PetSoundEngine(application.applicationContext)
    internal var aiService: DragosAiService = FirebaseDragosAiService()

    private val _uiState = MutableStateFlow(
        PetUiState(holdingOnePlus13 = rollHoldingPhone(PetRoom.LIVING))
    )
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    private var reactionResetJob: Job? = null
    private var speechResetJob: Job? = null
    private var foodAnimJob: Job? = null
    private var aiChatJob: Job? = null
    private var particleIdCounter = 0L
    private var chatMessageIdCounter = 0L
    private var washTapCount = 0
    private var lastWashTapMs = 0L

    private val headLines = listOf(
        "Careful with the glasses! Haha!",
        "Hey! You ruffled my hair!",
        "Boop! Did my glasses bounce?",
        "Whoa, tickles my ears!"
    )

    private val bellyLines = listOf(
        "Hahahaha! Stop, that tickles!",
        "Hey, my white shirt is clean!",
        "Hehe! Belly tickle attack!",
        "Haha, I can't stop laughing!"
    )

    private val footLines = listOf(
        "Watch the blue Nike pants!",
        "Whoa! Sporty hop!",
        "Hey, those are my lucky sneakers!",
        "Haha! Ready to sprint!"
    )

    private val petLines = listOf(
        "Aww, that feels so nice!",
        "Best friend ever!",
        "Yay! Head pats!",
        "I'm the happiest kid today!"
    )

    private val goodBoyLines = listOf(
        "Aww, really?! I'm a good boy!!",
        "Hehe, thank you! Being a good boy rules!",
        "Who's a good boy? Me! *adjusts glasses*",
        "Yay! You just made my whole day!"
    )

    init {
        loadSavedState()
        observeAudioFlows()
        startSimulationTicker()
    }

    private fun loadSavedState() {
        viewModelScope.launch {
            val saved = repository.petDataFlow.first()
            soundEngine.soundEnabled = saved.soundEnabled
            val elapsedMinutes = if (saved.lastSavedMs > 0L) {
                ((System.currentTimeMillis() - saved.lastSavedMs) / 60000f).coerceIn(0f, 120f)
            } else 0f

            val hunger = (saved.hunger - elapsedMinutes * 0.25f).coerceIn(25f, 100f)
            val happiness = (saved.happiness - elapsedMinutes * 0.18f).coerceIn(30f, 100f)
            val energy = (saved.energy + elapsedMinutes * 0.15f).coerceIn(30f, 100f)
            val freshness = (saved.freshness - elapsedMinutes * 0.15f).coerceIn(30f, 100f)

            _uiState.update {
                it.copy(
                    happiness = happiness,
                    hunger = hunger,
                    energy = energy,
                    freshness = freshness,
                    coins = saved.coins,
                    xp = saved.xp,
                    level = computeLevel(saved.xp),
                    soundEnabled = saved.soundEnabled,
                    highScoreKeepyUppy = saved.highScoreKeepyUppy
                )
            }
            delay(2200)
            if (_uiState.value.reaction == DragosReaction.WAVE) {
                _uiState.update { it.copy(reaction = DragosReaction.IDLE) }
            }
        }
    }

    private fun observeAudioFlows() {
        viewModelScope.launch {
            soundEngine.isListening.collect { listening ->
                _uiState.update { state ->
                    state.copy(
                        isListening = listening,
                        reaction = if (listening) DragosReaction.LISTENING
                        else if (state.reaction == DragosReaction.LISTENING && !state.isTalking) DragosReaction.IDLE
                        else state.reaction
                    )
                }
            }
        }
        viewModelScope.launch {
            soundEngine.isTalking.collect { talking ->
                _uiState.update { state ->
                    state.copy(
                        isTalking = talking,
                        reaction = if (talking) DragosReaction.TALKING
                        else if (state.reaction == DragosReaction.TALKING) DragosReaction.HAPPY_LAUGH
                        else state.reaction
                    )
                }
            }
        }
        viewModelScope.launch {
            soundEngine.micAmplitude.collect { amp ->
                _uiState.update { it.copy(micAmplitude = amp) }
            }
        }
        viewModelScope.launch {
            soundEngine.mouthAmplitude.collect { amp ->
                _uiState.update { it.copy(mouthAmplitude = amp) }
            }
        }
    }

    private fun startSimulationTicker() {
        viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(3000)
                tickCount++
                _uiState.update { state ->
                    if (state.lightsOff && state.currentRoom == PetRoom.BEDROOM) {
                        val newEnergy = (state.energy + 3.5f).coerceAtMost(100f)
                        if (tickCount % 2 == 0) {
                            spawnParticlesInternal(listOf("💤", "✨"), 1)
                            soundEngine.playEffect(PetSoundEffect.SNORE)
                        }
                        state.copy(
                            energy = newEnergy,
                            reaction = DragosReaction.SLEEPING
                        )
                    } else {
                        state.copy(
                            hunger = (state.hunger - 0.35f).coerceAtLeast(5f),
                            happiness = (state.happiness - 0.25f).coerceAtLeast(10f),
                            energy = (state.energy - 0.20f).coerceAtLeast(10f),
                            freshness = (state.freshness - 0.18f).coerceAtLeast(10f)
                        )
                    }
                }
                if (tickCount % 5 == 0) {
                    persistCurrentState()
                }
            }
        }
    }

    fun selectRoom(room: PetRoom) {
        soundEngine.cancelVoice()
        val holdsPhone = rollHoldingPhone(room)
        _uiState.update { state ->
            val sleeping = room == PetRoom.BEDROOM && state.lightsOff
            state.copy(
                currentRoom = room,
                isMiniGameActive = false,
                holdingOnePlus13 = holdsPhone,
                reaction = if (sleeping) DragosReaction.SLEEPING else DragosReaction.WAVE,
                speechBubble = when (room) {
                    PetRoom.LIVING -> if (holdsPhone) "Check out my blue OnePlus 13!" else "Welcome back! Wanna chat?"
                    PetRoom.KITCHEN -> "Ooh, I'm ready for a snack!"
                    PetRoom.PLAYROOM -> "Game time! Let's play!"
                    PetRoom.BEDROOM -> if (state.lightsOff) "Zzz… cozy dreams…" else "Time to relax or freshen up!"
                }
            )
        }
        if (!(room == PetRoom.BEDROOM && _uiState.value.lightsOff)) {
            scheduleReactionReset(1400)
        }
        scheduleSpeechClear(2600)
    }

    private fun rollHoldingPhone(room: PetRoom): Boolean =
        room == PetRoom.LIVING && Random.nextFloat() < 0.60f

    fun onBodyTap(zone: BodyZone) {
        if (_uiState.value.isListening || _uiState.value.isTalking) return
        if (_uiState.value.lightsOff && _uiState.value.currentRoom == PetRoom.BEDROOM) {
            toggleLights()
            return
        }

        if (_uiState.value.femboyMode) {
            val meowReaction = when (zone) {
                BodyZone.HEAD -> DragosReaction.GIGGLE_HEAD
                BodyZone.BELLY -> DragosReaction.BELLY_TICKLE
                BodyZone.FEET -> DragosReaction.FOOT_HOP
            }
            val meowLine = listOf("Meow~ :3", "Mrrrow~! :3", "Meow meow~ 💖").random()
            soundEngine.playEffect(PetSoundEffect.MEOW)
            awardProgress(happinessDelta = 5f, xpDelta = 5, coinDelta = 1)
            triggerReaction(meowReaction, meowLine, durationMs = 1500)
            spawnParticles(listOf("🐾", "💖", "✨"), count = 3)
            return
        }

        val (reaction, line, effect, emoji) = when (zone) {
            BodyZone.HEAD -> Quad(DragosReaction.GIGGLE_HEAD, headLines.random(), PetSoundEffect.GIGGLE, "🤓")
            BodyZone.BELLY -> Quad(DragosReaction.BELLY_TICKLE, bellyLines.random(), PetSoundEffect.GIGGLE, "😆")
            BodyZone.FEET -> Quad(DragosReaction.FOOT_HOP, footLines.random(), PetSoundEffect.BOING, "👟")
        }

        soundEngine.playEffect(effect)
        awardProgress(happinessDelta = 4f, xpDelta = 5, coinDelta = 1)
        triggerReaction(reaction, line, durationMs = 1500)
        spawnParticles(listOf(emoji, "✨"), count = 3)
    }

    fun onHeadPetting() {
        if (_uiState.value.isListening || _uiState.value.isTalking) return
        if (_uiState.value.lightsOff && _uiState.value.currentRoom == PetRoom.BEDROOM) return
        if (_uiState.value.femboyMode) {
            soundEngine.playEffect(PetSoundEffect.MEOW)
            awardProgress(happinessDelta = 6f, xpDelta = 6, coinDelta = 2)
            triggerReaction(DragosReaction.PETTING_PURR, "Meow~ purr :3", durationMs = 1700)
            spawnParticles(listOf("💖", "🐾", "🐱"), count = 4)
            return
        }
        soundEngine.playEffect(PetSoundEffect.PURR)
        awardProgress(happinessDelta = 6f, xpDelta = 6, coinDelta = 2)
        triggerReaction(DragosReaction.PETTING_PURR, petLines.random(), durationMs = 1700)
        spawnParticles(listOf("💙", "✨", "😊"), count = 4)
    }

    fun onAlexTapped() {
        soundEngine.playEffect(PetSoundEffect.GIGGLE)
        setSpeech("Alex: Hey! Just chilling in the back!")
        spawnParticles(listOf("👋", "😎"), count = 2)
        viewModelScope.launch {
            _uiState.update { it.copy(alexWaving = true) }
            delay(1600)
            _uiState.update { it.copy(alexWaving = false) }
        }
    }

    fun feedDragos(food: FoodItem) {
        val current = _uiState.value
        if (current.coins < food.cost) {
            soundEngine.playEffect(PetSoundEffect.BLEH)
            setSpeech("Need ${food.cost} coins! Play in the Playroom to earn more!")
            return
        }

        foodAnimJob?.cancel()
        foodAnimJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    coins = it.coins - food.cost,
                    activeFoodEmoji = food.emoji
                )
            }
            delay(360)
            _uiState.update { it.copy(activeFoodEmoji = null) }

            val effect = when (food.reaction) {
                DragosReaction.EATING_DISGUSTED -> PetSoundEffect.BLEH
                DragosReaction.SPICY_FIRE -> PetSoundEffect.SPICY
                else -> PetSoundEffect.YUMMY
            }
            soundEngine.playEffect(effect)

            val particleSet = when (food.reaction) {
                DragosReaction.SPICY_FIRE -> listOf("🔥", "💨", "🌶️")
                DragosReaction.EATING_DISGUSTED -> listOf("🥦", "😖")
                else -> listOf(food.emoji, "😋", "✨")
            }
            spawnParticles(particleSet, count = 4)

            _uiState.update { state ->
                val newXp = state.xp + 10
                state.copy(
                    hunger = (state.hunger + food.hungerGain).coerceIn(0f, 100f),
                    happiness = (state.happiness + food.happinessGain).coerceIn(0f, 100f),
                    energy = (state.energy + food.energyGain).coerceIn(0f, 100f),
                    freshness = (state.freshness - 2f).coerceAtLeast(10f),
                    xp = newXp,
                    level = computeLevel(newXp)
                )
            }
            triggerReaction(food.reaction, food.speechLine, durationMs = 2000)
            persistCurrentState()
        }
    }

    fun triggerAction(action: DragosReaction) {
        if (_uiState.value.lightsOff && _uiState.value.currentRoom == PetRoom.BEDROOM) {
            toggleLights()
        }
        when (action) {
            DragosReaction.DANCING -> {
                soundEngine.playEffect(PetSoundEffect.COIN)
                awardProgress(happinessDelta = 9f, energyDelta = -5f, xpDelta = 8, coinDelta = 2)
                triggerReaction(DragosReaction.DANCING, "Check out my dance moves!", 2200)
                spawnParticles(listOf("🎵", "🎶", "🕺"), count = 5)
            }
            DragosReaction.JUMPING -> {
                soundEngine.playEffect(PetSoundEffect.BOING)
                awardProgress(happinessDelta = 7f, energyDelta = -4f, xpDelta = 6, coinDelta = 1)
                triggerReaction(DragosReaction.JUMPING, "Boing! Super high jump!", 1500)
                spawnParticles(listOf("⚡", "⭐"), count = 3)
            }
            DragosReaction.SOCCER_KICK -> {
                soundEngine.playEffect(PetSoundEffect.KICK)
                awardProgress(happinessDelta = 8f, energyDelta = -5f, freshnessDelta = -3f, xpDelta = 8, coinDelta = 2)
                triggerReaction(DragosReaction.SOCCER_KICK, "GOOOAL! Blue Nike power!", 1600)
                spawnParticles(listOf("⚽", "🏆", "✨"), count = 4)
            }
            DragosReaction.WAVE -> {
                soundEngine.playEffect(PetSoundEffect.GIGGLE)
                awardProgress(happinessDelta = 4f, xpDelta = 3)
                triggerReaction(DragosReaction.WAVE, "Salut, friend! High five!", 1500)
                spawnParticles(listOf("👋", "😊"), count = 2)
            }
            DragosReaction.GOOD_BOY -> {
                if (_uiState.value.femboyMode) {
                    soundEngine.playEffect(PetSoundEffect.MEOW)
                    awardProgress(happinessDelta = 10f, xpDelta = 8, coinDelta = 2)
                    triggerReaction(DragosReaction.GOOD_BOY, "yes i am :3", 2400)
                    spawnParticles(listOf("🫠", "💖", "🐾", "🎀", "✨"), count = 6)
                } else {
                    soundEngine.playEffect(PetSoundEffect.PURR)
                    awardProgress(happinessDelta = 10f, xpDelta = 8, coinDelta = 2)
                    triggerReaction(DragosReaction.GOOD_BOY, goodBoyLines.random(), 2000)
                    spawnParticles(listOf("💙", "⭐", "🥰", "✨"), count = 5)
                }
            }
            else -> Unit
        }
    }

    fun washUp() {
        val now = System.currentTimeMillis()
        washTapCount = if (now - lastWashTapMs <= 2200L) washTapCount + 1 else 1
        lastWashTapMs = now
        val activateFemboy = washTapCount >= 5
        if (activateFemboy) {
            washTapCount = 0
        }

        soundEngine.playEffect(if (activateFemboy) PetSoundEffect.MEOW else PetSoundEffect.SPLASH)
        _uiState.update { state ->
            val newXp = state.xp + 10
            state.copy(
                freshness = 100f,
                happiness = (state.happiness + 8f).coerceAtMost(100f),
                lightsOff = false,
                xp = newXp,
                level = computeLevel(newXp),
                femboyMode = state.femboyMode || activateFemboy,
                showFemboyPopup = activateFemboy || state.showFemboyPopup
            )
        }
        if (activateFemboy) {
            triggerReaction(DragosReaction.HAPPY_LAUGH, "Meow~ :3", 2200)
            spawnParticles(listOf("🎀", "💖", "🐾", "✨"), count = 6)
        } else {
            triggerReaction(DragosReaction.REFRESHED, "Squeaky clean & glasses polished!", 2000)
            spawnParticles(listOf("🫧", "✨", "💧"), count = 6)
        }
        persistCurrentState()
    }

    fun dismissFemboyPopup() {
        _uiState.update { it.copy(showFemboyPopup = false) }
    }

    fun openAiChat() {
        _uiState.update { it.copy(isAiChatOpen = true, aiError = null) }
    }

    fun closeAiChat() {
        _uiState.update { it.copy(isAiChatOpen = false) }
    }

    fun clearAiError() {
        _uiState.update { it.copy(aiError = null) }
    }

    fun sendAiChatMessage(rawText: String) {
        val prompt = rawText.trim()
        if (prompt.isEmpty() || _uiState.value.isAiLoading) return

        val snapshot = _uiState.value
        val priorHistory = snapshot.chatMessages
        val userMsg = ChatMessage(
            id = ++chatMessageIdCounter,
            isFromUser = true,
            text = prompt,
            isFemboyMode = snapshot.femboyMode
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isAiLoading = true,
                streamingAiText = null,
                aiError = null
            )
        }

        if (snapshot.femboyMode && isGoodBoyPraise(prompt)) {
            soundEngine.playEffect(PetSoundEffect.MEOW)
            awardProgress(happinessDelta = 8f, xpDelta = 6, coinDelta = 1)
            triggerReaction(DragosReaction.GOOD_BOY, "yes i am :3", durationMs = 2400)
            spawnParticles(listOf("🫠", "💖", "🐾", "🎀"), count = 6)
        }

        launchAiStream(priorHistory = priorHistory, prompt = prompt, snapshot = snapshot)
    }

    fun retryLastAiMessage() {
        val snapshot = _uiState.value
        if (snapshot.isAiLoading) return
        val lastUserIndex = snapshot.chatMessages.indexOfLast { it.isFromUser }
        if (lastUserIndex < 0) return

        val lastUserMsg = snapshot.chatMessages[lastUserIndex]
        val priorHistory = snapshot.chatMessages.take(lastUserIndex)

        _uiState.update {
            it.copy(
                isAiLoading = true,
                streamingAiText = null,
                aiError = null
            )
        }
        launchAiStream(priorHistory = priorHistory, prompt = lastUserMsg.text, snapshot = snapshot)
    }

    private fun launchAiStream(
        priorHistory: List<ChatMessage>,
        prompt: String,
        snapshot: PetUiState
    ) {
        aiChatJob?.cancel()
        aiChatJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiService.streamReply(
                        history = priorHistory,
                        userMessage = prompt,
                        state = snapshot
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        _uiState.update { it.copy(streamingAiText = accumulated.toString()) }
                    }
                }
                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    val replyMsg = ChatMessage(
                        id = ++chatMessageIdCounter,
                        isFromUser = false,
                        text = finalReply,
                        isFemboyMode = _uiState.value.femboyMode
                    )
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + replyMsg,
                            isAiLoading = false,
                            streamingAiText = null
                        )
                    }
                    awardProgress(happinessDelta = 4f, xpDelta = 5)
                    persistCurrentState()
                } else {
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            streamingAiText = null,
                            aiError = "Gemini returned an empty response. Please try again."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("DragosViewModel", "Gemini chat timed out", e)
                _uiState.update {
                    it.copy(
                        isAiLoading = false,
                        streamingAiText = null,
                        aiError = "Gemini timed out after 60 seconds. Please try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DragosViewModel", "Gemini chat failed", e)
                _uiState.update {
                    it.copy(
                        isAiLoading = false,
                        streamingAiText = null,
                        aiError = e.localizedMessage ?: "Failed to chat with Gemini. Please try again."
                    )
                }
            }
        }
    }

    private fun isGoodBoyPraise(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("good boy") ||
            lower.contains("best boy") ||
            lower.contains("good kitty") ||
            lower.contains("good boi")
    }

    fun toggleLights() {
        val nextOff = !_uiState.value.lightsOff
        _uiState.update { state ->
            state.copy(
                lightsOff = nextOff,
                reaction = if (nextOff) DragosReaction.SLEEPING else DragosReaction.WAVE,
                speechBubble = if (nextOff) "Goodnight… recharging energy…" else "Good morning! Ready to play!"
            )
        }
        if (!nextOff) {
            soundEngine.playEffect(PetSoundEffect.COIN)
            scheduleReactionReset(1400)
        } else {
            soundEngine.playEffect(PetSoundEffect.SNORE)
        }
        scheduleSpeechClear(2600)
        persistCurrentState()
    }

    fun toggleVoiceRecording() {
        val state = _uiState.value
        if (state.isTalking) {
            soundEngine.cancelVoice()
            _uiState.update { it.copy(reaction = DragosReaction.IDLE) }
            return
        }
        if (state.isListening) {
            stopAndRepeatVoice()
        } else {
            if (state.lightsOff && state.currentRoom == PetRoom.BEDROOM) {
                toggleLights()
            }
            setSpeech("Listening… say something fun!")
            soundEngine.startListening(
                onAutoStop = { stopAndRepeatVoice() },
                onMicError = { msg -> setSpeech(msg) }
            )
        }
    }

    private fun stopAndRepeatVoice() {
        setSpeech("Haha, my turn!")
        soundEngine.stopListeningAndRepeat(
            onComplete = {
                awardProgress(happinessDelta = 7f, xpDelta = 10, coinDelta = 3)
                spawnParticles(listOf("🗣️", "✨", "😄"), count = 3)
                scheduleReactionReset(1200)
                scheduleSpeechClear(2200)
            },
            onTooShort = {
                soundEngine.playEffect(PetSoundEffect.GIGGLE)
                triggerReaction(DragosReaction.HAPPY_LAUGH, "Speak louder so I can copy you!", 1800)
            }
        )
    }

    fun startMiniGame() {
        soundEngine.cancelVoice()
        _uiState.update { it.copy(isMiniGameActive = true) }
    }

    fun finishMiniGame(score: Int, coinsEarned: Int) {
        soundEngine.playEffect(PetSoundEffect.COIN)
        _uiState.update { state ->
            val best = maxOf(state.highScoreKeepyUppy, score)
            val newXp = state.xp + (score / 2).coerceAtLeast(8)
            state.copy(
                isMiniGameActive = false,
                coins = state.coins + coinsEarned,
                happiness = (state.happiness + 18f).coerceAtMost(100f),
                energy = (state.energy - 10f).coerceAtLeast(10f),
                freshness = (state.freshness - 6f).coerceAtLeast(10f),
                highScoreKeepyUppy = best,
                xp = newXp,
                level = computeLevel(newXp),
                reaction = DragosReaction.HAPPY_LAUGH,
                speechBubble = "Awesome! Scored $score & earned +$coinsEarned coins!"
            )
        }
        spawnParticles(listOf("🏆", "🪙", "⚽"), count = 6)
        scheduleReactionReset(2200)
        scheduleSpeechClear(3200)
        persistCurrentState()
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        soundEngine.soundEnabled = next
        _uiState.update { it.copy(soundEnabled = next) }
        if (next) soundEngine.playEffect(PetSoundEffect.COIN)
        persistCurrentState()
    }

    private fun awardProgress(
        happinessDelta: Float = 0f,
        energyDelta: Float = 0f,
        freshnessDelta: Float = 0f,
        xpDelta: Int = 0,
        coinDelta: Int = 0
    ) {
        _uiState.update { state ->
            val newXp = state.xp + xpDelta
            state.copy(
                happiness = (state.happiness + happinessDelta).coerceIn(0f, 100f),
                energy = (state.energy + energyDelta).coerceIn(0f, 100f),
                freshness = (state.freshness + freshnessDelta).coerceIn(0f, 100f),
                coins = state.coins + coinDelta,
                xp = newXp,
                level = computeLevel(newXp)
            )
        }
    }

    private fun triggerReaction(reaction: DragosReaction, speech: String, durationMs: Long) {
        _uiState.update { it.copy(reaction = reaction, speechBubble = speech) }
        scheduleReactionReset(durationMs)
        scheduleSpeechClear(durationMs + 700)
    }

    private fun setSpeech(text: String) {
        _uiState.update { it.copy(speechBubble = text) }
        scheduleSpeechClear(2800)
    }

    private fun scheduleReactionReset(delayMs: Long) {
        reactionResetJob?.cancel()
        reactionResetJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { state ->
                if (!state.isListening && !state.isTalking && !(state.lightsOff && state.currentRoom == PetRoom.BEDROOM)) {
                    state.copy(reaction = DragosReaction.IDLE)
                } else state
            }
        }
    }

    private fun scheduleSpeechClear(delayMs: Long) {
        speechResetJob?.cancel()
        speechResetJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { it.copy(speechBubble = null) }
        }
    }

    private fun spawnParticles(emojis: List<String>, count: Int) {
        viewModelScope.launch {
            spawnParticlesInternal(emojis, count)
            delay(1300)
            _uiState.update { state ->
                state.copy(particles = state.particles.drop(count.coerceAtMost(state.particles.size)))
            }
        }
    }

    private fun spawnParticlesInternal(emojis: List<String>, count: Int) {
        val created = (0 until count).map { idx ->
            particleIdCounter++
            FloatingParticle(
                id = particleIdCounter,
                emoji = emojis[idx % emojis.size],
                xFraction = 0.28f + (idx * 0.14f) % 0.48f,
                yFraction = 0.35f + (idx % 3) * 0.08f
            )
        }
        _uiState.update { it.copy(particles = (it.particles + created).takeLast(12)) }
    }

    private fun computeLevel(xp: Int): Int = (xp / 100) + 1

    private fun persistCurrentState() {
        val s = _uiState.value
        viewModelScope.launch {
            repository.savePetData(
                PersistedPetData(
                    happiness = s.happiness,
                    hunger = s.hunger,
                    energy = s.energy,
                    freshness = s.freshness,
                    coins = s.coins,
                    xp = s.xp,
                    soundEnabled = s.soundEnabled,
                    highScoreKeepyUppy = s.highScoreKeepyUppy
                )
            )
        }
    }

    override fun onCleared() {
        persistCurrentState()
        soundEngine.release()
        super.onCleared()
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
