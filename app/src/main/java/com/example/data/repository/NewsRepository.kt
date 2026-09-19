package com.example.data.repository

import com.example.data.local.GoogleSheetsPreferences
import com.example.data.local.NewsArticleEntity
import com.example.data.local.NewsDao
import com.example.data.local.TopicEntity
import com.example.data.model.NewsArticle
import com.example.data.model.Topic
import com.example.data.remote.GoogleSheetsService
import com.example.data.remote.NewsApiService
import com.example.data.remote.SaveToSheetsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NewsRepository(
    private val newsDao: NewsDao,
    val apiService: NewsApiService = NewsApiService(),
    val sheetsService: GoogleSheetsService = GoogleSheetsService(),
    val sheetsPreferences: GoogleSheetsPreferences? = null
) {

    val topicsFlow: Flow<List<Topic>> = newsDao.getTopicsFlow().map { entities ->
        val userTopics = if (entities.isEmpty()) {
            Topic.DEFAULT_TOPICS
        } else {
            entities.map { it.toDomain() }
        }
        listOf(Topic.ALL_TOPIC) + userTopics
    }

    val allArticlesFlow: Flow<List<NewsArticle>> =
        newsDao.getAllArticlesFlow().map { list -> list.map { it.toDomain() } }

    val bookmarkedArticlesFlow: Flow<List<NewsArticle>> =
        newsDao.getBookmarkedArticlesFlow().map { list -> list.map { it.toDomain() } }

    fun getArticlesForTopicFlow(topicId: String): Flow<List<NewsArticle>> =
        if (topicId == Topic.ALL_TOPIC_ID) {
            allArticlesFlow
        } else {
            newsDao.getArticlesByTopicFlow(topicId).map { list -> list.map { it.toDomain() } }
        }

    suspend fun refreshAllTopics(topics: List<Topic>): Result<Unit> {
        val targets = topics.filter { !it.isAllTopic }
        var anySuccess = false
        var lastError: Throwable? = null

        for (t in targets) {
            val res = fetchAndCacheNews(t)
            if (res.isSuccess) {
                anySuccess = true
            } else {
                lastError = res.exceptionOrNull()
            }
        }

        return if (anySuccess) Result.success(Unit)
        else Result.failure(lastError ?: Exception("Failed to fetch news for topics"))
    }

    suspend fun initializeDefaultsIfNeeded() {
        val existing = newsDao.getAllTopicsSync()
        if (existing.isEmpty()) {
            newsDao.insertTopicsIfNotExist(Topic.DEFAULT_TOPICS.map { TopicEntity.fromDomain(it) })
        }
    }

    suspend fun fetchAndCacheNews(topic: Topic): Result<List<NewsArticle>> {
        val result = apiService.fetchNewsForTopic(
            topicId = topic.id,
            topicName = topic.name,
            query = topic.query
        )

        return if (result.isSuccess) {
            val articles = result.getOrNull() ?: emptyList()
            if (articles.isNotEmpty()) {
                val entities = articles.map { NewsArticleEntity.fromDomain(it) }
                newsDao.saveFetchedArticles(topic.id, entities)
            }
            Result.success(articles)
        } else {
            // Check if we have cached articles
            val exception = result.exceptionOrNull()
            // If cache is empty, save fallback articles so user has immediate content
            val fallback = apiService.getFallbackArticlesForTopic(topic.id, topic.name)
            val entities = fallback.map { NewsArticleEntity.fromDomain(it) }
            newsDao.saveFetchedArticles(topic.id, entities)
            Result.failure(exception ?: Exception("Failed to fetch news"))
        }
    }

    suspend fun addCustomTopic(name: String, query: String? = null): Topic {
        val trimmedName = name.trim()
        val searchQuery = if (query.isNullOrBlank()) trimmedName else query.trim()
        val id = "custom_" + UUID.randomUUID().toString().take(8)

        val newTopic = Topic(
            id = id,
            name = trimmedName,
            query = searchQuery,
            isDefault = false,
            iconName = "label",
            orderIndex = 100,
            createdAt = System.currentTimeMillis()
        )

        newsDao.insertTopic(TopicEntity.fromDomain(newTopic))
        // Fetch initial news for newly created topic
        fetchAndCacheNews(newTopic)
        return newTopic
    }

    suspend fun deleteCustomTopic(topicId: String) {
        newsDao.deleteTopicById(topicId)
        newsDao.clearUnsavedArticlesByTopic(topicId)
    }

    suspend fun toggleBookmark(articleId: String, currentStatus: Boolean) {
        val newStatus = !currentStatus
        val bookmarkedAt = if (newStatus) System.currentTimeMillis() else 0L
        newsDao.updateBookmarkStatus(articleId, newStatus, bookmarkedAt)
    }

    suspend fun saveArticleToGoogleSheets(
        article: NewsArticle,
        sheetCategory: String = "AI News"
    ): Result<SaveToSheetsResult> {
        val prefs = sheetsPreferences ?: return Result.failure(Exception("Google Sheets preferences not initialized"))

        // Webhook method
        if (prefs.webhookUrl.isNotBlank()) {
            val res = sheetsService.sendToWebhook(prefs.webhookUrl, article, sheetCategory)
            if (res.isSuccess) {
                newsDao.updateSheetsStatus(article.id, true, System.currentTimeMillis(), prefs.webhookUrl)
            }
            return res
        }

        // OAuth REST API v4 method
        val token = prefs.accessToken
        var spreadsheetId = prefs.spreadsheetId

        if (token.isNotBlank()) {
            if (spreadsheetId.isBlank()) {
                val createResult = sheetsService.createNewSpreadsheet(token)
                if (createResult.isSuccess) {
                    spreadsheetId = createResult.getOrThrow()
                    prefs.spreadsheetId = spreadsheetId
                } else {
                    return Result.failure(createResult.exceptionOrNull() ?: Exception("Failed to create Google Sheet"))
                }
            }

            val appendResult = sheetsService.appendArticleToSheet(token, spreadsheetId, article, sheetCategory)
            if (appendResult.isSuccess) {
                val url = sheetsService.getSpreadsheetWebUrl(spreadsheetId)
                prefs.lastSavedSpreadsheetUrl = url
                newsDao.updateSheetsStatus(article.id, true, System.currentTimeMillis(), url)
            }
            return appendResult
        }

        return Result.failure(Exception("Google Sheets is not connected yet. Please tap Connect or paste your token."))
    }

    suspend fun exportTableToGoogleSheets(
        sheetTab: String,
        rows: List<List<String>>
    ): Result<SaveToSheetsResult> {
        val prefs = sheetsPreferences ?: return Result.failure(Exception("Google Sheets preferences not initialized"))
        if (prefs.webhookUrl.isNotBlank()) {
            return sheetsService.sendDataRowsToWebhook(prefs.webhookUrl, sheetTab, rows)
        }
        return Result.failure(Exception("Please configure your Google Sheets Webhook URL in Sheets Settings to sync table data."))
    }

    suspend fun markArticleSavedToSheets(articleId: String, sheetUrl: String?) {
        newsDao.updateSheetsStatus(articleId, true, System.currentTimeMillis(), sheetUrl)
    }

    fun formatTsvForSheets(article: NewsArticle, sheetCategory: String = "AI News"): String {
        return sheetsService.formatTsvForClipboard(article, sheetCategory)
    }
}
