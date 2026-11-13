package com.example.model

enum class PetRoom {
    LIVING,
    KITCHEN,
    PLAYROOM,
    BEDROOM
}

enum class DragosReaction {
    IDLE,
    HAPPY_LAUGH,
    GIGGLE_HEAD,
    BELLY_TICKLE,
    FOOT_HOP,
    PETTING_PURR,
    LISTENING,
    TALKING,
    EATING_YUMMY,
    EATING_DISGUSTED,
    SPICY_FIRE,
    SLEEPING,
    DANCING,
    JUMPING,
    SOCCER_KICK,
    REFRESHED,
    WAVE,
    GOOD_BOY
}

enum class BodyZone {
    HEAD,
    BELLY,
    FEET
}

data class FoodItem(
    val id: String,
    val name: String,
    val emoji: String,
    val hungerGain: Float,
    val happinessGain: Float,
    val energyGain: Float,
    val cost: Int,
    val reaction: DragosReaction,
    val speechLine: String
)

data class FloatingParticle(
    val id: Long,
    val emoji: String,
    val xFraction: Float,
    val yFraction: Float
)

data class ChatMessage(
    val id: Long,
    val isFromUser: Boolean,
    val text: String,
    val isFemboyMode: Boolean = false
)

data class PetUiState(
    val currentRoom: PetRoom = PetRoom.LIVING,
    val happiness: Float = 86f,
    val hunger: Float = 78f,
    val energy: Float = 84f,
    val freshness: Float = 88f,
    val coins: Int = 60,
    val xp: Int = 20,
    val level: Int = 1,
    val reaction: DragosReaction = DragosReaction.WAVE,
    val speechBubble: String? = "Salut! I'm Dragoș! Tap me or talk to me!",
    val lightsOff: Boolean = false,
    val isListening: Boolean = false,
    val isTalking: Boolean = false,
    val mouthAmplitude: Float = 0f,
    val micAmplitude: Float = 0f,
    val soundEnabled: Boolean = true,
    val activeFoodEmoji: String? = null,
    val particles: List<FloatingParticle> = emptyList(),
    val isMiniGameActive: Boolean = false,
    val highScoreKeepyUppy: Int = 0,
    val holdingOnePlus13: Boolean = false,
    val alexWaving: Boolean = false,
    val femboyMode: Boolean = false,
    val showFemboyPopup: Boolean = false,
    val isAiChatOpen: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isAiLoading: Boolean = false,
    val streamingAiText: String? = null,
    val aiError: String? = null
)

object FoodCatalog {
    val items = listOf(
        FoodItem(
            id = "apple",
            name = "Crisp Apple",
            emoji = "🍎",
            hungerGain = 18f,
            happinessGain = 6f,
            energyGain = 4f,
            cost = 0,
            reaction = DragosReaction.EATING_YUMMY,
            speechLine = "Crunchy and sweet! Yum!"
        ),
        FoodItem(
            id = "pizza",
            name = "Pizza Slice",
            emoji = "🍕",
            hungerGain = 28f,
            happinessGain = 14f,
            energyGain = 6f,
            cost = 5,
            reaction = DragosReaction.EATING_YUMMY,
            speechLine = "Best pizza ever! Extra cheese!"
        ),
        FoodItem(
            id = "sarmale",
            name = "Sarmale",
            emoji = "🥟",
            hungerGain = 35f,
            happinessGain = 18f,
            energyGain = 10f,
            cost = 10,
            reaction = DragosReaction.EATING_YUMMY,
            speechLine = "Mmm, homemade sarmale! My favorite!"
        ),
        FoodItem(
            id = "milkshake",
            name = "Berry Shake",
            emoji = "🥤",
            hungerGain = 15f,
            happinessGain = 16f,
            energyGain = 12f,
            cost = 6,
            reaction = DragosReaction.EATING_YUMMY,
            speechLine = "Brain freeze! Haha, so good!"
        ),
        FoodItem(
            id = "croissant",
            name = "Croissant",
            emoji = "🥐",
            hungerGain = 22f,
            happinessGain = 10f,
            energyGain = 5f,
            cost = 4,
            reaction = DragosReaction.EATING_YUMMY,
            speechLine = "Flaky and warm! Thank you!"
        ),
        FoodItem(
            id = "broccoli",
            name = "Broccoli",
            emoji = "🥦",
            hungerGain = 20f,
            happinessGain = -8f,
            energyGain = 10f,
            cost = 0,
            reaction = DragosReaction.EATING_DISGUSTED,
            speechLine = "Blehh! Okay, healthy for my glasses..."
        ),
        FoodItem(
            id = "spinach",
            name = "Spinach",
            emoji = "🥬",
            hungerGain = 24f,
            happinessGain = 8f,
            energyGain = 22f,
            cost = 0,
            reaction = DragosReaction.EATING_YUMMY,
            speechLine = "Spinach power! I feel super strong!"
        ),
        FoodItem(
            id = "chili",
            name = "Hot Chili",
            emoji = "🌶️",
            hungerGain = 12f,
            happinessGain = 10f,
            energyGain = 18f,
            cost = 8,
            reaction = DragosReaction.SPICY_FIRE,
            speechLine = "WHOA! MY MOUTH IS ON FIRE!!"
        )
    )
}
