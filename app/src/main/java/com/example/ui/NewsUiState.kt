package com.example.ui

import com.example.data.model.AdvisoryFirm
import com.example.data.model.AppSection
import com.example.data.model.Fortune500Company
import com.example.data.model.LlmModelRanking
import com.example.data.model.NewsArticle
import com.example.data.model.Topic
import com.example.data.model.WorkAccount

data class NewsUiState(
    val isLandingScreenOpen: Boolean = true,
    val currentSection: AppSection = AppSection.AI_NEWS,
    // AI News
    val topics: List<Topic> = listOf(Topic.ALL_TOPIC) + Topic.DEFAULT_TOPICS,
    val selectedTopic: Topic? = Topic.ALL_TOPIC,
    val articles: List<NewsArticle> = emptyList(),

    // Work Accounts
    val availableWorkAccounts: List<WorkAccount> = WorkAccount.DEFAULT_WORK_ACCOUNTS,
    val selectedWorkAccountIds: Set<String> = WorkAccount.DEFAULT_WORK_ACCOUNTS.take(6).map { it.id }.toSet(),
    val workAccountArticles: List<NewsArticle> = emptyList(),
    val activeWorkAccountFilter: String = "All Selected",

    // Advisory Firms
    val advisoryFirms: List<AdvisoryFirm> = listOf(AdvisoryFirm.ALL_FIRMS) + AdvisoryFirm.DEFAULT_FIRMS,
    val selectedAdvisoryFirm: AdvisoryFirm = AdvisoryFirm.ALL_FIRMS,
    val advisoryArticles: List<NewsArticle> = emptyList(),

    // Fortune 500 Index
    val fortune500List: List<Fortune500Company> = Fortune500Company.TOP_COMPANIES,
    val fortune500SearchQuery: String = "",

    // LLM Rankings
    val llmRankingsList: List<LlmModelRanking> = LlmModelRanking.TOP_LLM_LEADERBOARD,
    val llmSearchQuery: String = "",

    // Bookmarks & Global
    val bookmarkedArticles: List<NewsArticle> = emptyList(),
    val isBookmarksView: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdatedIst: String = "",
    val selectedArticleForDetail: NewsArticle? = null,
    val isAddTopicDialogOpen: Boolean = false,
    val isManageWorkAccountsDialogOpen: Boolean = false,
    val isSettingsPreferencesDialogOpen: Boolean = false,

    // Google Sheets Integration
    val selectedArticleForSheets: NewsArticle? = null,
    val selectedArticleSheetCategory: String = "AI News",
    val isSavingToSheets: Boolean = false,
    val sheetsStatusMessage: String? = null,
    val lastSavedSheetsUrl: String? = null,
    val sheetsSpreadsheetId: String = "",
    val sheetsAccessToken: String = "",
    val sheetsWebhookUrl: String = "",

    // Supabase Auth & Role-Based Access Control
    val isAuthenticated: Boolean = false,
    val currentUserEmail: String = "",
    val currentUserRole: com.example.data.model.UserRole = com.example.data.model.UserRole.USER,
    val currentAccessStatus: com.example.data.model.AccessStatus = com.example.data.model.AccessStatus.NONE,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val authSuccessMessage: String? = null,
    val isSupabaseConfigOpen: Boolean = false,
    val isAdminPanelOpen: Boolean = false,
    val pendingAccessRequests: List<com.example.data.model.AccessRequest> = emptyList(),
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = ""
) {
    val displayedArticles: List<NewsArticle>
        get() {
            if (isBookmarksView) {
                if (searchQuery.isBlank()) return bookmarkedArticles
                val query = searchQuery.trim().lowercase()
                return bookmarkedArticles.filter { article ->
                    article.title.lowercase().contains(query) ||
                        article.description.lowercase().contains(query) ||
                        article.source.lowercase().contains(query) ||
                        article.topicName.lowercase().contains(query)
                }
            }

            val sourceList = when (currentSection) {
                AppSection.AI_NEWS -> articles
                AppSection.WORK_ACCOUNTS -> workAccountArticles
                AppSection.ADVISORY_FIRMS -> advisoryArticles
                else -> emptyList()
            }

            if (searchQuery.isBlank()) return sourceList
            val query = searchQuery.trim().lowercase()
            return sourceList.filter { article ->
                article.title.lowercase().contains(query) ||
                    article.description.lowercase().contains(query) ||
                    article.source.lowercase().contains(query) ||
                    article.topicName.lowercase().contains(query)
            }
        }

    val filteredFortune500: List<Fortune500Company>
        get() {
            if (fortune500SearchQuery.isBlank()) return fortune500List
            val q = fortune500SearchQuery.trim().lowercase()
            return fortune500List.filter {
                it.name.lowercase().contains(q) ||
                    it.ticker.lowercase().contains(q) ||
                    it.sector.lowercase().contains(q) ||
                    it.ceo.lowercase().contains(q)
            }
        }

    val filteredLlmRankings: List<LlmModelRanking>
        get() {
            if (llmSearchQuery.isBlank()) return llmRankingsList
            val q = llmSearchQuery.trim().lowercase()
            return llmRankingsList.filter {
                it.modelName.lowercase().contains(q) ||
                    it.provider.lowercase().contains(q) ||
                    it.strengths.lowercase().contains(q)
            }
        }
}
