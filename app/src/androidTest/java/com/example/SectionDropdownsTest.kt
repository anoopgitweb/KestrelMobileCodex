package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.AdvisoryFirm
import com.example.data.model.WorkAccount
import com.example.ui.components.AdvisoryFirmsBar
import com.example.ui.components.SectionFilterDropdown
import com.example.ui.components.WorkAccountsBar
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SectionDropdownsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun workAccountsOpenAsMultiSelectDropdown() {
        compose.setContent {
            MyApplicationTheme {
                WorkAccountsBar(
                    accounts = WorkAccount.DEFAULT_WORK_ACCOUNTS.take(3),
                    selectedAccountIds = setOf("acc_msft", "acc_goog"),
                    onToggleAccount = {},
                    onOpenManageDialog = {}
                )
            }
        }
        compose.onNodeWithTag("work_accounts_dropdown_trigger").performClick()
        compose.onNodeWithTag("work_accounts_dropdown_menu").assertIsDisplayed()
        compose.onNodeWithText("Microsoft").assertIsDisplayed()
    }

    @Test
    fun advisoryFirmsOpenAsDropdown() {
        val firms = listOf(AdvisoryFirm.ALL_FIRMS) + AdvisoryFirm.DEFAULT_FIRMS.take(2)
        compose.setContent {
            MyApplicationTheme {
                AdvisoryFirmsBar(firms, firms.first(), onSelectFirm = {})
            }
        }
        compose.onNodeWithTag("advisory_dropdown_trigger").performClick()
        compose.onNodeWithTag("advisory_dropdown_menu").assertIsDisplayed()
        compose.onNodeWithText("Forrester Research").assertIsDisplayed()
    }

    @Test
    fun providerFilterOpensAsDropdown() {
        compose.setContent {
            MyApplicationTheme {
                SectionFilterDropdown(
                    label = "Select LLM Provider",
                    allLabel = "All providers",
                    options = listOf("OpenAI", "Google"),
                    selectedOption = null,
                    icon = Icons.Default.Leaderboard,
                    testTagPrefix = "llm_provider",
                    onSelect = {}
                )
            }
        }
        compose.onNodeWithTag("llm_provider_dropdown_trigger").performClick()
        compose.onNodeWithTag("llm_provider_dropdown_menu").assertIsDisplayed()
        compose.onNodeWithText("OpenAI").assertIsDisplayed()
    }
}
