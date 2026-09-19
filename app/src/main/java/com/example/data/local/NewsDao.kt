package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    // Topics
    @Query("SELECT * FROM topics ORDER BY isDefault DESC, orderIndex ASC, createdAt ASC")
    fun getTopicsFlow(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY isDefault DESC, orderIndex ASC, createdAt ASC")
    suspend fun getAllTopicsSync(): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopicsIfNotExist(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :topicId AND isDefault = 0")
    suspend fun deleteTopicById(topicId: String)

    // Articles
    @Query("SELECT * FROM news_articles ORDER BY publishedAtMillis DESC")
    fun getAllArticlesFlow(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE topicId = :topicId ORDER BY publishedAtMillis DESC")
    fun getArticlesByTopicFlow(topicId: String): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE isBookmarked = 1 ORDER BY bookmarkedAtMillis DESC, publishedAtMillis DESC")
    fun getBookmarkedArticlesFlow(): Flow<List<NewsArticleEntity>>

    @Query("SELECT id FROM news_articles WHERE isBookmarked = 1")
    suspend fun getBookmarkedArticleIds(): List<String>

    @Query("SELECT * FROM news_articles WHERE isSavedToSheets = 1")
    suspend fun getSheetsSavedEntities(): List<NewsArticleEntity>

    @Query("UPDATE news_articles SET isSavedToSheets = :isSaved, sheetsSavedAtMillis = :savedAtMillis, sheetsUrl = :sheetsUrl WHERE id = :articleId")
    suspend fun updateSheetsStatus(articleId: String, isSaved: Boolean, savedAtMillis: Long, sheetsUrl: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("UPDATE news_articles SET isBookmarked = :isBookmarked, bookmarkedAtMillis = :bookmarkedAtMillis WHERE id = :articleId")
    suspend fun updateBookmarkStatus(articleId: String, isBookmarked: Boolean, bookmarkedAtMillis: Long)

    @Query("DELETE FROM news_articles WHERE topicId = :topicId AND isBookmarked = 0 AND isSavedToSheets = 0")
    suspend fun clearUnsavedArticlesByTopic(topicId: String)

    @Transaction
    suspend fun saveFetchedArticles(topicId: String, fetched: List<NewsArticleEntity>) {
        val bookmarkedIds = getBookmarkedArticleIds().toSet()
        val sheetsSavedMap = getSheetsSavedEntities().associateBy { it.id }
        val mapped = fetched.map { article ->
            var updated = article
            if (bookmarkedIds.contains(article.id)) {
                updated = updated.copy(isBookmarked = true)
            }
            val sheetsEntity = sheetsSavedMap[article.id]
            if (sheetsEntity != null) {
                updated = updated.copy(
                    isSavedToSheets = true,
                    sheetsSavedAtMillis = sheetsEntity.sheetsSavedAtMillis,
                    sheetsUrl = sheetsEntity.sheetsUrl
                )
            }
            updated
        }
        insertArticles(mapped)
    }
}
