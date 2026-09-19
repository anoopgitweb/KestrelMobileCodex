package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AuthPreferences
import com.example.data.local.GoogleSheetsPreferences
import com.example.data.model.AccessRequest
import com.example.data.model.AccessStatus
import com.example.data.model.AdvisoryFirm
import com.example.data.model.AppSection
import com.example.data.model.Fortune500Company
import com.example.data.model.LlmModelRanking
import com.example.data.model.NewsArticle
import com.example.data.model.Topic
import com.example.data.model.UserRole
import com.example.data.model.WorkAccount
import com.example.data.remote.SupabaseAuthService
import com.example.data.repository.NewsRepository
import com.example.util.IstTimeUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NewsRepository
    private val sheetsPrefs: GoogleSheetsPreferences = GoogleSheetsPreferences(application)
    private val authPrefs: AuthPreferences = AuthPreferences(application)
    val authService: SupabaseAuthService = SupabaseAuthService(authPrefs)
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var articlesJob: Job? = null
    private var workAccountsJob: Job? = null
    private var advisoryJob: Job? = null

    init {
        val database = AppDatabase.getInstance(application)
        repository = NewsRepository(
            newsDao = database.newsDao(),
            sheetsPreferences = sheetsPrefs
        )

        // Parse saved work accounts
        val savedAccountNames = sheetsPrefs.selectedWorkAccountsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val customAccountNames = sheetsPrefs.customWorkAccountsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val customAccounts = customAccountNames.map { name ->
            WorkAccount("custom_${name.lowercase().replace(" ", "_")}", name, "", "Custom Work Account", true)
        }
        val allAccounts = WorkAccount.DEFAULT_WORK_ACCOUNTS + customAccounts
        val selectedIds = allAccounts.filter { savedAccountNames.contains(it.name) }.map { it.id }.toSet()

        // Load saved session and role
        val savedEmail = authPrefs.userEmail
        val savedRole = if (authPrefs.userRole == "admin" || authService.isAdmin(savedEmail)) UserRole.ADMIN else UserRole.USER
        val savedStatus = try {
            AccessStatus.valueOf(authPrefs.accessStatus)
        } catch (_: Exception) {
            if (savedRole == UserRole.ADMIN) AccessStatus.APPROVED else AccessStatus.NONE
        }
        val isApproved = savedRole == UserRole.ADMIN || (savedStatus == AccessStatus.APPROVED && authPrefs.isEmailApproved(savedEmail))
        val isUserLoggedIn = authPrefs.isLoggedIn && isApproved

        // Load saved Sheets preferences & Auth state
        _uiState.update {
            it.copy(
                sheetsSpreadsheetId = sheetsPrefs.spreadsheetId,
                sheetsAccessToken = sheetsPrefs.accessToken,
                sheetsWebhookUrl = sheetsPrefs.webhookUrl,
                lastSavedSheetsUrl = sheetsPrefs.lastSavedSpreadsheetUrl,
                availableWorkAccounts = allAccounts,
                selectedWorkAccountIds = if (selectedIds.isNotEmpty()) selectedIds else allAccounts.take(5).map { a -> a.id }.toSet(),
                isAuthenticated = isUserLoggedIn,
                currentUserEmail = savedEmail,
                currentUserRole = savedRole,
                currentAccessStatus = if (isApproved) AccessStatus.APPROVED else savedStatus,
                supabaseUrl = authPrefs.supabaseUrl,
                supabaseAnonKey = authPrefs.supabaseAnonKey
            )
        }

        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }

        // Observe Topics from repository
        viewModelScope.launch {
            repository.topicsFlow.collectLatest { topicList ->
                _uiState.update { current ->
                    val updatedList = if (topicList.isEmpty()) {
                        listOf(Topic.ALL_TOPIC) + Topic.DEFAULT_TOPICS
                    } else {
                        topicList
                    }
                    val currentSelected = current.selectedTopic
                    val newSelected = if (currentSelected != null && updatedList.any { it.id == currentSelected.id }) {
                        updatedList.first { it.id == currentSelected.id }
                    } else {
                        updatedList.firstOrNull() ?: Topic.ALL_TOPIC
                    }
                    current.copy(
                        topics = updatedList,
                        selectedTopic = newSelected
                    )
                }

                // If not observing articles yet, attach to selected topic
                val currentSelected = _uiState.value.selectedTopic
                if (currentSelected != null && articlesJob == null) {
                    observeArticlesForTopic(currentSelected)
                    fetchNewsForTopic(currentSelected, isPullToRefresh = false)
                }
            }
        }

        // Observe Bookmarked articles
        viewModelScope.launch {
            repository.bookmarkedArticlesFlow.collectLatest { bookmarks ->
                _uiState.update { it.copy(bookmarkedArticles = bookmarks) }
            }
        }

        // Initial fetch for Work Accounts and Advisory Firms in background
        fetchWorkAccountsNews(false)
        fetchAdvisoryNews(_uiState.value.selectedAdvisoryFirm, false)
    }

    fun selectSection(section: AppSection) {
        _uiState.update {
            it.copy(
                isLandingScreenOpen = false,
                currentSection = section,
                isBookmarksView = false,
                searchQuery = "",
                errorMessage = null
            )
        }
        when (section) {
            AppSection.AI_NEWS -> {
                val topic = _uiState.value.selectedTopic ?: Topic.ALL_TOPIC
                observeArticlesForTopic(topic)
            }
            AppSection.WORK_ACCOUNTS -> {
                if (_uiState.value.workAccountArticles.isEmpty()) {
                    fetchWorkAccountsNews(false)
                }
            }
            AppSection.ADVISORY_FIRMS -> {
                if (_uiState.value.advisoryArticles.isEmpty()) {
                    fetchAdvisoryNews(_uiState.value.selectedAdvisoryFirm, false)
                }
            }
            AppSection.FORTUNE_500 -> {
                // Table view
            }
            AppSection.LLM_RANKINGS -> {
                // Benchmark view
            }
        }
    }

    fun openLandingScreen() {
        _uiState.update { it.copy(isLandingScreenOpen = true) }
    }

    fun closeLandingScreen() {
        _uiState.update { it.copy(isLandingScreenOpen = false) }
    }

    fun selectTopic(topic: Topic) {
        if (_uiState.value.selectedTopic?.id == topic.id && !_uiState.value.isBookmarksView) {
            return
        }

        _uiState.update {
            it.copy(
                selectedTopic = topic,
                isBookmarksView = false,
                searchQuery = "",
                errorMessage = null
            )
        }
        observeArticlesForTopic(topic)
        fetchNewsForTopic(topic, isPullToRefresh = false)
    }

    private fun observeArticlesForTopic(topic: Topic) {
        articlesJob?.cancel()
        articlesJob = viewModelScope.launch {
            repository.getArticlesForTopicFlow(topic.id).collectLatest { articleList ->
                _uiState.update { current ->
                    current.copy(
                        articles = articleList,
                        isLoading = if (articleList.isNotEmpty()) false else current.isLoading
                    )
                }
            }
        }
    }

    fun refreshCurrentSection() {
        when (_uiState.value.currentSection) {
            AppSection.AI_NEWS -> refreshCurrentTopic()
            AppSection.WORK_ACCOUNTS -> fetchWorkAccountsNews(true)
            AppSection.ADVISORY_FIRMS -> fetchAdvisoryNews(_uiState.value.selectedAdvisoryFirm, true)
            AppSection.FORTUNE_500 -> {
                _uiState.update { it.copy(lastUpdatedIst = IstTimeUtil.formatToIstTimeOnly(System.currentTimeMillis())) }
            }
            AppSection.LLM_RANKINGS -> {
                _uiState.update { it.copy(lastUpdatedIst = IstTimeUtil.formatToIstTimeOnly(System.currentTimeMillis())) }
            }
        }
    }

    fun refreshCurrentTopic() {
        val topic = _uiState.value.selectedTopic ?: return
        fetchNewsForTopic(topic, isPullToRefresh = true)
    }

    private fun fetchNewsForTopic(topic: Topic, isPullToRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isPullToRefresh) it.copy(isRefreshing = true)
                else if (it.articles.isEmpty()) it.copy(isLoading = true)
                else it
            }

            val result = if (topic.isAllTopic) {
                repository.refreshAllTopics(_uiState.value.topics)
            } else {
                repository.fetchAndCacheNews(topic).map { }
            }

            _uiState.update { current ->
                val errorMsg = if (result.isFailure && current.articles.isEmpty()) {
                    "Unable to fetch online news. Showing cached content."
                } else null

                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = errorMsg,
                    lastUpdatedIst = IstTimeUtil.formatToIstTimeOnly(System.currentTimeMillis())
                )
            }
        }
    }

    // Work Accounts Logic
    fun toggleWorkAccountSelection(accountId: String) {
        val currentIds = _uiState.value.selectedWorkAccountIds.toMutableSet()
        if (currentIds.contains(accountId)) {
            if (currentIds.size > 1) { // keep at least 1
                currentIds.remove(accountId)
            }
        } else {
            currentIds.add(accountId)
        }
        val selectedNames = _uiState.value.availableWorkAccounts
            .filter { currentIds.contains(it.id) }
            .joinToString(",") { it.name }
        sheetsPrefs.selectedWorkAccountsRaw = selectedNames

        _uiState.update { it.copy(selectedWorkAccountIds = currentIds) }
        fetchWorkAccountsNews(isPullToRefresh = false)
    }

    fun addCustomWorkAccount(name: String) {
        if (name.isBlank()) return
        val cleanName = name.trim()
        val currentCustom = sheetsPrefs.customWorkAccountsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (!currentCustom.contains(cleanName)) {
            currentCustom.add(cleanName)
            sheetsPrefs.customWorkAccountsRaw = currentCustom.joinToString(",")
        }
        val customAccount = WorkAccount(
            id = "custom_${cleanName.lowercase().replace(" ", "_")}",
            name = cleanName,
            tickerOrDomain = "",
            industry = "Custom Work Account",
            isSelected = true
        )
        val updatedAccounts = _uiState.value.availableWorkAccounts + customAccount
        val updatedSelected = _uiState.value.selectedWorkAccountIds + customAccount.id

        val selectedNames = updatedAccounts.filter { updatedSelected.contains(it.id) }.joinToString(",") { it.name }
        sheetsPrefs.selectedWorkAccountsRaw = selectedNames

        _uiState.update {
            it.copy(
                availableWorkAccounts = updatedAccounts,
                selectedWorkAccountIds = updatedSelected
            )
        }
        fetchWorkAccountsNews(isPullToRefresh = false)
    }

    fun setManageWorkAccountsDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isManageWorkAccountsDialogOpen = open) }
    }

    fun fetchWorkAccountsNews(isPullToRefresh: Boolean = false) {
        workAccountsJob?.cancel()
        workAccountsJob = viewModelScope.launch {
            _uiState.update {
                if (isPullToRefresh) it.copy(isRefreshing = true)
                else if (it.workAccountArticles.isEmpty()) it.copy(isLoading = true)
                else it
            }

            val selectedAccounts = _uiState.value.availableWorkAccounts
                .filter { _uiState.value.selectedWorkAccountIds.contains(it.id) }
                .map { it.name }

            val res = repository.apiService.fetchNewsForWorkAccounts(selectedAccounts)
            _uiState.update { current ->
                val articleList: List<NewsArticle> = res.getOrDefault(emptyList())
                val mappedArticles = articleList.map { art ->
                    art.copy(topicName = "Work Accounts", topicId = "work_accounts")
                }
                current.copy(
                    workAccountArticles = if (mappedArticles.isNotEmpty()) mappedArticles else current.workAccountArticles,
                    isLoading = false,
                    isRefreshing = false,
                    lastUpdatedIst = IstTimeUtil.formatToIstTimeOnly(System.currentTimeMillis())
                )
            }
        }
    }

    // Advisory Firms Logic
    fun selectAdvisoryFirm(firm: AdvisoryFirm) {
        _uiState.update { it.copy(selectedAdvisoryFirm = firm) }
        fetchAdvisoryNews(firm, isPullToRefresh = false)
    }

    fun fetchAdvisoryNews(firm: AdvisoryFirm, isPullToRefresh: Boolean = false) {
        advisoryJob?.cancel()
        advisoryJob = viewModelScope.launch {
            _uiState.update {
                if (isPullToRefresh) it.copy(isRefreshing = true)
                else if (it.advisoryArticles.isEmpty()) it.copy(isLoading = true)
                else it
            }

            val res = repository.apiService.fetchNewsForAdvisory(firm.name, firm.searchQuery)
            _uiState.update { current ->
                val articleList: List<NewsArticle> = res.getOrDefault(emptyList())
                val mappedArticles = articleList.map { art ->
                    art.copy(topicName = firm.tag, topicId = firm.id)
                }
                current.copy(
                    advisoryArticles = if (mappedArticles.isNotEmpty()) mappedArticles else current.advisoryArticles,
                    isLoading = false,
                    isRefreshing = false,
                    lastUpdatedIst = IstTimeUtil.formatToIstTimeOnly(System.currentTimeMillis())
                )
            }
        }
    }

    // Search queries for Fortune 500 and LLMs
    fun setFortune500SearchQuery(query: String) {
        _uiState.update { it.copy(fortune500SearchQuery = query) }
    }

    fun setLlmSearchQuery(query: String) {
        _uiState.update { it.copy(llmSearchQuery = query) }
    }

    fun addCustomTopic(name: String, customQuery: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newTopic = repository.addCustomTopic(name, customQuery)
                _uiState.update { it.copy(isAddTopicDialogOpen = false) }
                selectTopic(newTopic)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to add topic: ${e.message}")
                }
            }
        }
    }

    fun deleteCustomTopic(topic: Topic) {
        if (topic.isDefault || topic.isAllTopic) return
        viewModelScope.launch {
            repository.deleteCustomTopic(topic.id)
            val fallbackTopic = _uiState.value.topics.firstOrNull { it.id != topic.id }
                ?: Topic.ALL_TOPIC
            selectTopic(fallbackTopic)
        }
    }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch {
            repository.toggleBookmark(article.id, article.isBookmarked)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleBookmarksView() {
        _uiState.update {
            it.copy(
                isBookmarksView = !it.isBookmarksView,
                searchQuery = ""
            )
        }
    }

    fun selectArticleForDetail(article: NewsArticle?) {
        _uiState.update { it.copy(selectedArticleForDetail = article) }
    }

    fun setAddTopicDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddTopicDialogOpen = open) }
    }

    fun setSettingsPreferencesDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsPreferencesDialogOpen = open) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Google Sheets Actions
    fun openSaveToSheetsDialog(article: NewsArticle, sheetCategory: String? = null) {
        val category = sheetCategory ?: _uiState.value.currentSection.sheetTabName
        _uiState.update {
            it.copy(
                selectedArticleForSheets = article,
                selectedArticleSheetCategory = category,
                sheetsStatusMessage = null
            )
        }
    }

    fun closeSaveToSheetsDialog() {
        _uiState.update {
            it.copy(
                selectedArticleForSheets = null,
                sheetsStatusMessage = null
            )
        }
    }

    fun updateSheetsConfig(spreadsheetId: String, accessToken: String, webhookUrl: String) {
        sheetsPrefs.spreadsheetId = spreadsheetId
        sheetsPrefs.accessToken = accessToken
        sheetsPrefs.webhookUrl = webhookUrl
        _uiState.update {
            it.copy(
                sheetsSpreadsheetId = spreadsheetId,
                sheetsAccessToken = accessToken,
                sheetsWebhookUrl = webhookUrl
            )
        }
    }

    fun saveArticleToGoogleSheets(
        article: NewsArticle,
        sheetCategory: String = _uiState.value.selectedArticleSheetCategory,
        onComplete: ((Boolean, String, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToSheets = true, sheetsStatusMessage = "Saving article to '$sheetCategory' tab...") }
            val result = repository.saveArticleToGoogleSheets(article, sheetCategory)
            _uiState.update { current ->
                if (result.isSuccess) {
                    val res = result.getOrThrow()
                    current.copy(
                        isSavingToSheets = false,
                        sheetsStatusMessage = res.message,
                        lastSavedSheetsUrl = res.spreadsheetUrl,
                        sheetsSpreadsheetId = sheetsPrefs.spreadsheetId
                    )
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Failed to save to Google Sheets"
                    current.copy(
                        isSavingToSheets = false,
                        sheetsStatusMessage = errMsg
                    )
                }
            }

            if (result.isSuccess) {
                val res = result.getOrThrow()
                onComplete?.invoke(true, res.message, res.spreadsheetUrl)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "Failed to save to Google Sheets"
                onComplete?.invoke(false, errMsg, null)
            }
        }
    }

    fun exportFortune500ToSheets(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToSheets = true, sheetsStatusMessage = "Exporting Fortune 500 Index to Google Sheets...") }
            val rows = _uiState.value.fortune500List.map {
                listOf(
                    it.rank.toString(),
                    it.name,
                    it.ticker,
                    it.sector,
                    "$${it.revenueBillions}B",
                    "$${it.profitBillions}B",
                    "$${it.marketCapBillions}B",
                    it.ceo,
                    it.headquarters,
                    it.keyHighlight,
                    IstTimeUtil.formatToIstFull(System.currentTimeMillis())
                )
            }
            val res = repository.exportTableToGoogleSheets("Fortune 500", rows)
            val msg = if (res.isSuccess) res.getOrThrow().message else res.exceptionOrNull()?.message ?: "Export failed"
            _uiState.update { it.copy(isSavingToSheets = false, sheetsStatusMessage = msg) }
            onComplete?.invoke(res.isSuccess, msg)
        }
    }

    fun exportLlmRankingsToSheets(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToSheets = true, sheetsStatusMessage = "Exporting LLM Rankings to Google Sheets...") }
            val rows = _uiState.value.llmRankingsList.map {
                listOf(
                    it.rank.toString(),
                    it.modelName,
                    it.provider,
                    it.arenaElo.toString(),
                    "${it.mmluScore}%",
                    it.contextWindowTokens,
                    "$${it.costPerMillionInputTokens}",
                    "$${it.costPerMillionOutputTokens}",
                    it.strengths,
                    it.license,
                    IstTimeUtil.formatToIstFull(System.currentTimeMillis())
                )
            }
            val res = repository.exportTableToGoogleSheets("LLM Rankings", rows)
            val msg = if (res.isSuccess) res.getOrThrow().message else res.exceptionOrNull()?.message ?: "Export failed"
            _uiState.update { it.copy(isSavingToSheets = false, sheetsStatusMessage = msg) }
            onComplete?.invoke(res.isSuccess, msg)
        }
    }

    fun markArticleSavedLocally(article: NewsArticle, sheetsUrl: String?) {
        viewModelScope.launch {
            repository.markArticleSavedToSheets(article.id, sheetsUrl)
        }
    }

    fun formatTsvForClipboard(article: NewsArticle, sheetCategory: String = "AI News"): String {
        return repository.formatTsvForSheets(article, sheetCategory)
    }

    fun getOAuthConsentUrl(): String {
        return repository.sheetsService.buildOAuthConsentUrl()
    }

    fun clearSheetsStatusMessage() {
        _uiState.update { it.copy(sheetsStatusMessage = null) }
    }

    // Supabase Auth & Role-Based Access Control Actions
    fun loginWithSupabase(email: String, password: String, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = authService.signIn(email, password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                val isApproved = user.role == UserRole.ADMIN || user.status == AccessStatus.APPROVED
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        isAuthenticated = isApproved,
                        currentUserEmail = user.email,
                        currentUserRole = user.role,
                        currentAccessStatus = user.status,
                        authErrorMessage = if (!isApproved && user.status == AccessStatus.PENDING) {
                            "Your access request is pending approval by the Administrator."
                        } else if (!isApproved && user.status == AccessStatus.REJECTED) {
                            "Access denied. Please contact the Administrator."
                        } else null,
                        authSuccessMessage = if (isApproved) {
                            if (user.role == UserRole.ADMIN) "Welcome back, Administrator!" else "Access granted! Welcome to Enterprise AI Intelligence."
                        } else null
                    )
                }

                if (user.role == UserRole.ADMIN) {
                    loadAdminRequests()
                }

                onResult?.invoke(isApproved, if (isApproved) "Success" else "Access requires Administrator approval")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Authentication failed"
                _uiState.update { it.copy(isAuthLoading = false, authErrorMessage = err) }
                onResult?.invoke(false, err)
            }
        }
    }

    fun submitAccessRequest(
        fullName: String,
        email: String,
        organization: String,
        reason: String,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = authService.submitAccessRequest(fullName, email, organization, reason)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        currentUserEmail = email.trim().lowercase(),
                        currentAccessStatus = AccessStatus.PENDING,
                        authSuccessMessage = "Request submitted! The Administrator (${authService.adminEmail}) will review and grant access."
                    )
                }
                onComplete?.invoke(true, "Request submitted for approval")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to submit request"
                _uiState.update { it.copy(isAuthLoading = false, authErrorMessage = err) }
                onComplete?.invoke(false, err)
            }
        }
    }

    fun loadAdminRequests() {
        viewModelScope.launch {
            val requests = authService.getAccessRequests()
            _uiState.update { it.copy(pendingAccessRequests = requests) }
        }
    }

    fun updateAccessRequestStatus(requestId: String, email: String, newStatus: AccessStatus) {
        viewModelScope.launch {
            val result = authService.updateAccessStatus(requestId, email, newStatus)
            if (result.isSuccess) {
                loadAdminRequests()
            }
        }
    }

    fun updateSupabaseConfig(url: String, anonKey: String) {
        authPrefs.supabaseUrl = url.trim()
        authPrefs.supabaseAnonKey = anonKey.trim()
        _uiState.update {
            it.copy(
                supabaseUrl = url.trim(),
                supabaseAnonKey = anonKey.trim(),
                isSupabaseConfigOpen = false
            )
        }
    }

    fun setSupabaseConfigOpen(open: Boolean) {
        _uiState.update { it.copy(isSupabaseConfigOpen = open) }
    }

    fun setAdminPanelOpen(open: Boolean) {
        if (open) {
            loadAdminRequests()
        }
        _uiState.update { it.copy(isAdminPanelOpen = open) }
    }

    fun signOut() {
        authService.signOut()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                currentUserEmail = "",
                currentUserRole = UserRole.USER,
                currentAccessStatus = AccessStatus.NONE,
                isAdminPanelOpen = false,
                isLandingScreenOpen = true,
                authSuccessMessage = null,
                authErrorMessage = null
            )
        }
    }

    fun clearAuthMessages() {
        _uiState.update { it.copy(authErrorMessage = null, authSuccessMessage = null) }
    }
}

