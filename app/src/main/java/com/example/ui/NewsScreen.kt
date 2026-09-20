package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSection
import com.example.data.model.NewsArticle
import com.example.data.model.Topic
import com.example.data.model.UserRole
import com.example.ui.components.AddTopicDialog
import com.example.ui.components.AdminAccessControlDialog
import com.example.ui.components.AdvisoryFirmsBar
import com.example.ui.components.AppSectionNavigationBar
import com.example.ui.components.ArticleDetailSheet
import com.example.ui.components.Fortune500View
import com.example.ui.components.LandingScreen
import com.example.ui.components.LearnView
import com.example.ui.components.LlmRankingsView
import com.example.ui.components.LoginScreen
import com.example.ui.components.ManageWorkAccountsDialog
import com.example.ui.components.NewsCard
import com.example.ui.components.SaveToSheetsDialog
import com.example.ui.components.SectionFilterDropdown
import com.example.ui.components.SettingsPreferencesDialog
import com.example.ui.components.SupabaseConfigDialog
import com.example.ui.components.TopicPicker
import com.example.ui.components.WorkAccountsBar
import com.example.util.IstTimeUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isSearchActive by remember { mutableStateOf(false) }
    var topicToDelete by remember { mutableStateOf<Topic?>(null) }
    var currentIstClock by remember { mutableStateOf(IstTimeUtil.getCurrentIstTimeString()) }

    // Live clock ticker for IST
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L)
            currentIstClock = IstTimeUtil.getCurrentIstTimeString()
        }
    }

    // If user is not authenticated, display Supabase Login & Access Request Screen
    if (!uiState.isAuthenticated) {
        LoginScreen(
            adminEmail = viewModel.authService.adminEmail,
            isLoading = uiState.isAuthLoading,
            errorMessage = uiState.authErrorMessage,
            successMessage = uiState.authSuccessMessage,
            currentAccessStatus = uiState.currentAccessStatus,
            pinUnlockAvailable = uiState.isPinUnlockAvailable,
            savedEmail = uiState.currentUserEmail,
            onLogin = { email, pass, pin ->
                viewModel.loginWithSupabase(email, pass, pin)
            },
            onPinUnlock = { pin -> viewModel.unlockWithPin(pin) },
            onRequestAccess = { name, email, org, reason ->
                viewModel.submitAccessRequest(name, email, org, reason)
            },
            onOpenSupabaseConfig = {
                viewModel.setSupabaseConfigOpen(true)
            },
            onClearMessages = {
                viewModel.clearAuthMessages()
            },
            modifier = modifier
        )

        // Supabase Config Dialog on Login Screen
        if (uiState.isSupabaseConfigOpen) {
            SupabaseConfigDialog(
                initialUrl = uiState.supabaseUrl,
                initialAnonKey = uiState.supabaseAnonKey,
                onSave = { url, anonKey ->
                    viewModel.updateSupabaseConfig(url, anonKey)
                },
                onDismiss = { viewModel.setSupabaseConfigOpen(false) }
            )
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Kestrel Business Intelligence",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setSettingsPreferencesDialogOpen(true) },
                            modifier = Modifier.testTag("top_bar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings and preferences",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

                // App Sections Navigation Bar (Always visible after login across the 5 options)
                AppSectionNavigationBar(
                    currentSection = uiState.currentSection,
                    onSectionSelected = { viewModel.selectSection(it) }
                )

                // Search Bar (Animated visibility)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Filter articles by keyword, source...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input")
                        )
                    }
                }

                // Section-Specific Secondary Pickers
                if (!uiState.isBookmarksView && !uiState.isLandingScreenOpen) {
                    when (uiState.currentSection) {
                        AppSection.AI_NEWS -> {
                            TopicPicker(
                                topics = uiState.topics,
                                selectedTopic = uiState.selectedTopic,
                                isBookmarksView = uiState.isBookmarksView,
                                onTopicSelected = { viewModel.selectTopic(it) },
                                onAddTopicClicked = { viewModel.setAddTopicDialogOpen(true) },
                                onDeleteTopicClicked = { topicToDelete = it }
                            )
                        }
                        AppSection.WORK_ACCOUNTS -> {
                            WorkAccountsBar(
                                accounts = uiState.availableWorkAccounts,
                                selectedAccountIds = uiState.selectedWorkAccountIds,
                                onToggleAccount = { viewModel.toggleWorkAccountSelection(it) },
                                onOpenManageDialog = { viewModel.setManageWorkAccountsDialogOpen(true) }
                            )
                        }
                        AppSection.ADVISORY_FIRMS -> {
                            AdvisoryFirmsBar(
                                firms = uiState.advisoryFirms,
                                selectedFirm = uiState.selectedAdvisoryFirm,
                                onSelectFirm = { viewModel.selectAdvisoryFirm(it) }
                            )
                        }
                        AppSection.FORTUNE_500 -> {
                            val sectors = uiState.fortune500List.map { it.sector }.distinct().sorted()
                            val selectedSector = uiState.fortune500SearchQuery.takeIf { query ->
                                sectors.any { it.equals(query, ignoreCase = true) }
                            }
                            SectionFilterDropdown(
                                label = "Select Fortune 500 Sector",
                                allLabel = "All sectors",
                                options = sectors,
                                selectedOption = selectedSector,
                                icon = Icons.Default.TrendingUp,
                                testTagPrefix = "fortune_sector",
                                onSelect = { viewModel.setFortune500SearchQuery(it.orEmpty()) }
                            )
                        }
                        AppSection.LLM_RANKINGS -> {
                            val providers = uiState.llmRankingsList.map { it.provider }.distinct().sorted()
                            val selectedProvider = uiState.llmSearchQuery.takeIf { query ->
                                providers.any { it.equals(query, ignoreCase = true) }
                            }
                            SectionFilterDropdown(
                                label = "Select LLM Provider",
                                allLabel = "All providers",
                                options = providers,
                                selectedOption = selectedProvider,
                                icon = Icons.Default.Leaderboard,
                                testTagPrefix = "llm_provider",
                                onSelect = { viewModel.setLlmSearchQuery(it.orEmpty()) }
                            )
                        }
                        AppSection.LEARN -> Unit
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.openLandingScreen() }, modifier = Modifier.testTag("top_bar_landing_button")) {
                            Icon(Icons.Default.GridView, "Kestrel overview", tint = if (uiState.isLandingScreenOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        }, modifier = Modifier.testTag("top_bar_search_button")) {
                            Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, "Search")
                        }
                        IconButton(onClick = { viewModel.toggleBookmarksView() }, modifier = Modifier.testTag("top_bar_bookmarks_button")) {
                            BadgedBox(badge = { if (uiState.bookmarkedArticles.isNotEmpty()) Badge { Text("${uiState.bookmarkedArticles.size}") } }) {
                                Icon(if (uiState.isBookmarksView) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmarks")
                            }
                        }
                        IconButton(onClick = { viewModel.refreshCurrentSection() }, enabled = !uiState.isRefreshing, modifier = Modifier.testTag("top_bar_refresh_button")) {
                            if (uiState.isRefreshing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            else Icon(Icons.Default.Refresh, "Refresh")
                        }
                        if (uiState.currentUserRole == UserRole.ADMIN) {
                            val pendingApprovalsCount = uiState.pendingAccessRequests.count { it.status == com.example.data.model.AccessStatus.PENDING }
                            IconButton(onClick = { viewModel.setAdminPanelOpen(true) }, modifier = Modifier.testTag("top_bar_admin_button")) {
                                BadgedBox(badge = { if (pendingApprovalsCount > 0) Badge(containerColor = MaterialTheme.colorScheme.error) { Text("$pendingApprovalsCount") } }) {
                                    Icon(Icons.Default.AdminPanelSettings, "Access Approvals Panel", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.signOut() }, modifier = Modifier.testTag("top_bar_sign_out_button")) {
                            Icon(Icons.Default.ExitToApp, "Sign Out", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLandingScreenOpen) {
                LandingScreen(
                    currentIstTime = currentIstClock,
                    monitoredAccountsCount = uiState.selectedWorkAccountIds.size,
                    onSelectSection = { section ->
                        viewModel.selectSection(section)
                    }
                )
            } else {
                when (uiState.currentSection) {
                    AppSection.FORTUNE_500 -> {
                        Fortune500View(
                            companies = uiState.filteredFortune500,
                            searchQuery = uiState.fortune500SearchQuery,
                            onSearchChange = { viewModel.setFortune500SearchQuery(it) }
                        )
                    }
                    AppSection.LLM_RANKINGS -> {
                        LlmRankingsView(
                            models = uiState.filteredLlmRankings,
                            searchQuery = uiState.llmSearchQuery,
                            onSearchChange = { viewModel.setLlmSearchQuery(it) }
                        )
                    }
                    AppSection.LEARN -> LearnView()
                    AppSection.AI_NEWS, AppSection.WORK_ACCOUNTS, AppSection.ADVISORY_FIRMS -> {
                        val displayedArticles = uiState.displayedArticles

                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refreshCurrentSection() },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pull_to_refresh_container")
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("news_feed_list")
                            ) {
                                // Section Header
                                item {
                                    FeedHeader(
                                        isBookmarksView = uiState.isBookmarksView,
                                        selectedTopic = uiState.selectedTopic,
                                        articleCount = displayedArticles.size,
                                        lastUpdatedIst = uiState.lastUpdatedIst,
                                        onBackToFeed = { viewModel.toggleBookmarksView() },
                                        onDeleteCustomTopic = {
                                            uiState.selectedTopic?.let { topicToDelete = it }
                                        }
                                    )
                                }

                                // Error Banner if needed
                                if (uiState.errorMessage != null) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("error_banner")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = uiState.errorMessage ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(onClick = { viewModel.refreshCurrentSection() }) {
                                                    Text("Retry")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Loading State
                                if (uiState.isLoading && displayedArticles.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator()
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = when (uiState.currentSection) {
                                                        AppSection.WORK_ACCOUNTS -> "Fetching latest from work accounts (${uiState.selectedWorkAccountIds.size} monitored)..."
                                                        AppSection.ADVISORY_FIRMS -> "Fetching latest from ${uiState.selectedAdvisoryFirm.name}..."
                                                        else -> if (uiState.selectedTopic?.isAllTopic == true) {
                                                            "Fetching latest stories across all topics..."
                                                        } else {
                                                            "Fetching latest ${uiState.selectedTopic?.name ?: "AI"} news..."
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else if (displayedArticles.isEmpty()) {
                                    // Empty State
                                    item {
                                        EmptyFeedState(
                                            isBookmarksView = uiState.isBookmarksView,
                                            hasSearch = uiState.searchQuery.isNotEmpty(),
                                            onAction = {
                                                if (uiState.isBookmarksView) {
                                                    viewModel.toggleBookmarksView()
                                                } else if (uiState.searchQuery.isNotEmpty()) {
                                                    viewModel.setSearchQuery("")
                                                } else {
                                                    viewModel.refreshCurrentSection()
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    // News Articles List
                                    items(displayedArticles, key = { it.id }) { article ->
                                        NewsCard(
                                            article = article,
                                            onClick = { viewModel.selectArticleForDetail(article) },
                                            onBookmarkToggle = { viewModel.toggleBookmark(article) },
                                            onShare = { shareArticle(context, article) },
                                            onSaveToSheets = { viewModel.openSaveToSheetsDialog(article) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Article Detail Modal Bottom Sheet
            uiState.selectedArticleForDetail?.let { article ->
                ArticleDetailSheet(
                    article = article,
                    onDismiss = { viewModel.selectArticleForDetail(null) },
                    onOpenUrl = { url -> openWebUrl(context, url) },
                    onBookmarkToggle = { viewModel.toggleBookmark(article) },
                    onShare = { shareArticle(context, article) },
                    onSaveToSheets = { viewModel.openSaveToSheetsDialog(article) }
                )
            }

            // Save to Google Sheets Dialog
            uiState.selectedArticleForSheets?.let { article ->
                SaveToSheetsDialog(
                    article = article,
                    sheetCategory = uiState.selectedArticleSheetCategory,
                    isSaving = uiState.isSavingToSheets,
                    statusMessage = uiState.sheetsStatusMessage,
                    lastSavedUrl = uiState.lastSavedSheetsUrl,
                    savedSpreadsheetId = uiState.sheetsSpreadsheetId,
                    savedAccessToken = uiState.sheetsAccessToken,
                    savedWebhookUrl = uiState.sheetsWebhookUrl,
                    onDismiss = { viewModel.closeSaveToSheetsDialog() },
                    onSaveToSheets = { targetArticle, category ->
                        viewModel.saveArticleToGoogleSheets(targetArticle, category)
                    },
                    onUpdateConfig = { id, token, webhook ->
                        viewModel.updateSheetsConfig(id, token, webhook)
                    },
                    onGetOAuthUrl = { viewModel.getOAuthConsentUrl() },
                    onFormatTsv = { targetArticle, category -> viewModel.formatTsvForClipboard(targetArticle, category) },
                    onMarkSaved = { targetArticle, url -> viewModel.markArticleSavedLocally(targetArticle, url) }
                )
            }

            // Manage Work Accounts Preferences Dialog
            if (uiState.isManageWorkAccountsDialogOpen) {
                ManageWorkAccountsDialog(
                    availableAccounts = uiState.availableWorkAccounts,
                    selectedAccountIds = uiState.selectedWorkAccountIds,
                    onToggleAccount = { viewModel.toggleWorkAccountSelection(it) },
                    onAddCustomAccount = { viewModel.addCustomWorkAccount(it) },
                    onDeleteAccount = { viewModel.deleteWorkAccount(it) },
                    onDismiss = { viewModel.setManageWorkAccountsDialogOpen(false) }
                )
            }

            if (uiState.isSettingsPreferencesDialogOpen) {
                SettingsPreferencesDialog(
                    topics = uiState.topics,
                    selectedTopic = uiState.selectedTopic,
                    onSelectTopic = { viewModel.selectTopic(it) },
                    onAddTopic = { name, query -> viewModel.addCustomTopic(name, query) },
                    onDeleteTopic = { viewModel.deleteCustomTopic(it) },
                    availableWorkAccounts = uiState.availableWorkAccounts,
                    selectedWorkAccountIds = uiState.selectedWorkAccountIds,
                    onToggleWorkAccount = { viewModel.toggleWorkAccountSelection(it) },
                    onAddCustomWorkAccount = { viewModel.addCustomWorkAccount(it) },
                    onDeleteWorkAccount = { viewModel.deleteWorkAccount(it) },
                    onDismiss = { viewModel.setSettingsPreferencesDialogOpen(false) }
                )
            }

            // Add Custom Topic Dialog
            if (uiState.isAddTopicDialogOpen) {
                AddTopicDialog(
                    onDismiss = { viewModel.setAddTopicDialogOpen(false) },
                    onTopicAdded = { name, query ->
                        viewModel.addCustomTopic(name, query)
                        Toast.makeText(context, "Added topic: $name", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Delete Custom Topic Confirmation Dialog
            topicToDelete?.let { topic ->
                AlertDialog(
                    onDismissRequest = { topicToDelete = null },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    title = { Text("Delete Topic?") },
                    text = {
                        Text("Are you sure you want to remove \"${topic.name}\"? You can always add it again later.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteCustomTopic(topic)
                                topicToDelete = null
                                Toast.makeText(context, "Removed ${topic.name}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { topicToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Admin Access Control Dialog
            if (uiState.isAdminPanelOpen) {
                AdminAccessControlDialog(
                    requests = uiState.pendingAccessRequests,
                    onApprove = { reqId, email ->
                        viewModel.updateAccessRequestStatus(reqId, email, com.example.data.model.AccessStatus.APPROVED)
                        Toast.makeText(context, "Approved access for $email", Toast.LENGTH_SHORT).show()
                    },
                    onReject = { reqId, email ->
                        viewModel.updateAccessRequestStatus(reqId, email, com.example.data.model.AccessStatus.REJECTED)
                        Toast.makeText(context, "Revoked/rejected access for $email", Toast.LENGTH_SHORT).show()
                    },
                    onRefresh = { viewModel.loadAdminRequests() },
                    onDismiss = { viewModel.setAdminPanelOpen(false) }
                )
            }
        }
    }
}

@Composable
private fun FeedHeader(
    isBookmarksView: Boolean,
    selectedTopic: Topic?,
    articleCount: Int,
    lastUpdatedIst: String,
    onBackToFeed: () -> Unit,
    onDeleteCustomTopic: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isBookmarksView) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onBackToFeed)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back to Topics",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Saved Articles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (selectedTopic?.isAllTopic == true) "All Topics" else (selectedTopic?.name ?: "All Topics"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedTopic?.isAllTopic == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Unified Feed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (selectedTopic != null && !selectedTopic.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (selectedTopic?.isAllTopic == true) "$articleCount stories across all topics" else "$articleCount articles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lastUpdatedIst.isNotBlank() && !isBookmarksView) {
                    Text(
                        text = " • Updated $lastUpdatedIst",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Option to delete custom topic
        if (!isBookmarksView && selectedTopic != null && !selectedTopic.isDefault) {
            IconButton(
                onClick = onDeleteCustomTopic,
                modifier = Modifier.testTag("delete_custom_topic_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete custom topic",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyFeedState(
    isBookmarksView: Boolean,
    hasSearch: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isBookmarksView -> Icons.Default.BookmarkBorder
                        hasSearch -> Icons.Default.Search
                        else -> Icons.Default.Newspaper
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                isBookmarksView -> "No Saved Articles Yet"
                hasSearch -> "No Articles Match Your Search"
                else -> "No News Articles Found"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                isBookmarksView -> "Tap the bookmark icon on any article to save it for offline reading."
                hasSearch -> "Try a different search query or clear the filter."
                else -> "Pull to refresh to fetch the latest stories from Google News."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onAction) {
            Text(
                text = when {
                    isBookmarksView -> "Explore Topics"
                    hasSearch -> "Clear Filter"
                    else -> "Refresh News"
                }
            )
        }
    }
}

private fun openWebUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareArticle(context: Context, article: NewsArticle) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "${article.title}\n\n${article.source} • ${article.istFullTimestamp}\n\nRead more: ${article.link}"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share AI News Story")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share story: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
