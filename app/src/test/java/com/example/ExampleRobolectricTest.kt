package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.model.FoodCatalog
import com.example.model.PetRoom
import com.example.viewmodel.DragosViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dragoș Pet", appName)
    }

    @Test
    fun `spinach is available in food catalog`() {
        assertTrue(FoodCatalog.items.any { it.id == "spinach" && it.name == "Spinach" })
    }

    @Test
    fun `living room has roughly 60 percent chance of holding blue OnePlus 13`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = DragosViewModel(app)
        var heldCount = 0
        val trials = 200
        repeat(trials) {
            vm.selectRoom(PetRoom.LIVING)
            if (vm.uiState.value.holdingOnePlus13) {
                heldCount++
            }
        }
        assertTrue("Expected ~60% (got $heldCount/$trials)", heldCount in 80..160)

        vm.selectRoom(PetRoom.KITCHEN)
        assertFalse(vm.uiState.value.holdingOnePlus13)
    }

    @Test
    fun `dragos character and rooms render and switch cleanly`() {
        composeRule.onNodeWithTag("dragos_character").assertIsDisplayed()
        composeRule.onNodeWithTag("alex_background_character").assertIsDisplayed()
        composeRule.onNodeWithTag("talk_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("action_good_boy_button").assertIsDisplayed().performClick()

        // Switch to Kitchen and verify food tray includes Apple, Pizza, and Spinach
        composeRule.onNodeWithTag("room_tab_kitchen").performClick()
        composeRule.onNodeWithTag("alex_background_character").assertDoesNotExist()
        composeRule.onNodeWithTag("food_item_apple").assertIsDisplayed()
        composeRule.onNodeWithTag("food_item_pizza").assertIsDisplayed()
        composeRule.onNodeWithTag("kitchen_food_tray")
            .performScrollToNode(hasTestTag("food_item_spinach"))
        composeRule.onNodeWithTag("food_item_spinach").assertIsDisplayed()

        // Switch to Playroom and verify mini-game launch button
        composeRule.onNodeWithTag("room_tab_playroom").performClick()
        composeRule.onNodeWithTag("start_minigame_button").assertIsDisplayed()

        // Switch to Bedroom and verify lights toggle & wash up
        composeRule.onNodeWithTag("room_tab_bedroom").performClick()
        composeRule.onNodeWithTag("toggle_lights_button").assertIsDisplayed()
        composeRule.onNodeWithTag("wash_up_button").assertIsDisplayed()
        composeRule.onNodeWithText("Lights Off").assertIsDisplayed()
    }

    @Test
    fun `tapping wash up 5 times activates femboy mode with custom responses`() {
        composeRule.onNodeWithTag("room_tab_bedroom").performClick()

        // Press Wash Up 5 times quickly
        repeat(5) {
            composeRule.onNodeWithTag("wash_up_button").performClick()
        }

        // Verify popup with exact text appears and dismiss it
        composeRule.onNodeWithTag("femboy_mode_popup").assertIsDisplayed()
        composeRule.onNodeWithText("femboy mode activated").assertIsDisplayed()
        composeRule.onNodeWithTag("dismiss_femboy_popup_button").performClick()
        composeRule.onNodeWithTag("femboy_mode_popup").assertDoesNotExist()

        // Return to Living Room and test Good Boy + touching Dragoș
        composeRule.onNodeWithTag("room_tab_living").performClick()
        composeRule.onNodeWithTag("action_good_boy_button").performClick()
        composeRule.onNodeWithText("yes i am :3").assertIsDisplayed()

        composeRule.onNodeWithTag("dragos_character").performClick()
        composeRule.onNodeWithText("Meow", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `ai button opens and closes Gemini chat sheet and reflects femboy mode`() {
        composeRule.onNodeWithTag("ai_chat_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("ai_chat_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Powered by Gemini").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_chat_input").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_chip_good_boy").assertIsDisplayed()
        composeRule.onNodeWithTag("close_ai_chat_button").performClick()
        composeRule.onNodeWithTag("ai_chat_sheet").assertDoesNotExist()
    }

    @Test
    fun `femboy mode chat triggers melting good boy reaction and streams reply`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = DragosViewModel(app)
        var capturedFemboyFlag = false
        vm.aiService = object : com.example.ai.DragosAiService {
            override fun streamReply(
                history: List<com.example.model.ChatMessage>,
                userMessage: String,
                state: com.example.model.PetUiState
            ) = kotlinx.coroutines.flow.flow {
                capturedFemboyFlag = state.femboyMode
                emit("*melts into a blushing pink puddle* ")
                emit("nyaaa~!! yes i am :3")
            }
        }

        // Activate femboy mode via 5 quick Wash Up taps
        repeat(5) { vm.washUp() }
        assertTrue(vm.uiState.value.femboyMode)

        vm.openAiChat()
        vm.sendAiChatMessage("You're such a good boy!")
        // Wait briefly for coroutine on Main/IO
        val deadline = System.currentTimeMillis() + 3000L
        while (vm.uiState.value.isAiLoading && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        assertTrue(capturedFemboyFlag)
        assertEquals(com.example.model.DragosReaction.GOOD_BOY, vm.uiState.value.reaction)
        assertEquals("yes i am :3", vm.uiState.value.speechBubble)
        assertTrue(vm.uiState.value.chatMessages.any { !it.isFromUser && it.text.contains("melts") })
    }

    @Test
    fun `ai chat surfaces errors and supports retry without duplicating user turn`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = DragosViewModel(app)
        var attempt = 0
        vm.aiService = object : com.example.ai.DragosAiService {
            override fun streamReply(
                history: List<com.example.model.ChatMessage>,
                userMessage: String,
                state: com.example.model.PetUiState
            ) = kotlinx.coroutines.flow.flow {
                attempt++
                if (attempt == 1) {
                    throw IllegalStateException("Temporary network stall")
                }
                emit("Back online! Let's play soccer!")
            }
        }

        vm.openAiChat()
        vm.sendAiChatMessage("Hey Dragos!")
        val deadline1 = System.currentTimeMillis() + 3000L
        while (vm.uiState.value.isAiLoading && System.currentTimeMillis() < deadline1) {
            Thread.sleep(20)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        assertEquals("Temporary network stall", vm.uiState.value.aiError)
        assertEquals(1, vm.uiState.value.chatMessages.size)

        vm.retryLastAiMessage()
        val deadline2 = System.currentTimeMillis() + 3000L
        while (vm.uiState.value.isAiLoading && System.currentTimeMillis() < deadline2) {
            Thread.sleep(20)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        assertEquals(null, vm.uiState.value.aiError)
        assertEquals(2, vm.uiState.value.chatMessages.size)
        assertEquals(1, vm.uiState.value.chatMessages.count { it.isFromUser })
        assertTrue(vm.uiState.value.chatMessages.last().text.contains("Back online"))
    }
}
