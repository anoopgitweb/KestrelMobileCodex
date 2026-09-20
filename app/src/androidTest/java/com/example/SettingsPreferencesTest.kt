package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.Topic
import com.example.data.model.WorkAccount
import com.example.ui.components.SettingsPreferencesDialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPreferencesTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun anyWorkAccountCanBeDeletedFromSettings() {
        val customAccount = WorkAccount("custom_acme", "Acme", industry = "Custom Work Account")
        var deletedAccount: WorkAccount? = null

        compose.setContent {
            MyApplicationTheme {
                SettingsPreferencesDialog(
                    topics = listOf(Topic.ALL_TOPIC) + Topic.DEFAULT_TOPICS,
                    selectedTopic = Topic.ALL_TOPIC,
                    onSelectTopic = {},
                    onAddTopic = { _, _ -> },
                    onDeleteTopic = {},
                    availableWorkAccounts = listOf(WorkAccount.DEFAULT_WORK_ACCOUNTS.first(), customAccount),
                    selectedWorkAccountIds = setOf(customAccount.id),
                    onToggleWorkAccount = {},
                    onAddCustomWorkAccount = {},
                    onDeleteWorkAccount = { deletedAccount = it },
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithText("Work Accounts").performClick()
        val builtInAccount = WorkAccount.DEFAULT_WORK_ACCOUNTS.first()
        compose.onNodeWithTag("delete_work_account_${builtInAccount.id}").performClick()
        compose.runOnIdle { assertEquals(builtInAccount, deletedAccount) }
    }
}
