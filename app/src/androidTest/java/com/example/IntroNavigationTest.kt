package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun exploreLeavesIntroAndRemainsDismissedAfterRecreation() {
        compose.onNodeWithTag("intro_screen").assertIsDisplayed()
        compose.onNodeWithTag("intro_explore").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("intro_screen").assertDoesNotExist()
        compose.onNodeWithTag("login_email_input").assertExists()

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("intro_screen").assertDoesNotExist()
        compose.onNodeWithTag("login_email_input").assertExists()
    }
}
