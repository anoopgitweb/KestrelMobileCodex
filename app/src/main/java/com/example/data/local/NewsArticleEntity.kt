package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.NewsArticle

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val sourceUrl: String?,
    val publishedAtMillis: Long,
    val rawPublishedDate: String?,
    val topicId: String,
    val topicName: String,
    val isBookmarked: Boolean = false,
    val bookmarkedAtMillis: Long = 0L,
    val isSavedToSheets: Boolean = false,
    val sheetsSavedAtMillis: Long = 0L,
    val sheetsUrl: String? = null,
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): NewsArticle = NewsArticle(
        id = id,
        title = title,
        description = description,
        link = link,
        source = source,
        sourceUrl = sourceUrl,
        publishedAtMillis = publishedAtMillis,
        rawPublishedDate = rawPublishedDate,
        topicId = topicId,
        topicName = topicName,
        isBookmarked = isBookmarked,
        isSavedToSheets = isSavedToSheets,
        sheetsSavedAtMillis = sheetsSavedAtMillis,
        sheetsUrl = sheetsUrl,
        cachedAtMillis = cachedAtMillis
    )

    companion object {
        fun fromDomain(article: NewsArticle): NewsArticleEntity = NewsArticleEntity(
            id = article.id,
            title = article.title,
            description = article.description,
            link = article.link,
            source = article.source,
            sourceUrl = article.sourceUrl,
            publishedAtMillis = article.publishedAtMillis,
            rawPublishedDate = article.rawPublishedDate,
            topicId = article.topicId,
            topicName = article.topicName,
            isBookmarked = article.isBookmarked,
            isSavedToSheets = article.isSavedToSheets,
            sheetsSavedAtMillis = article.sheetsSavedAtMillis,
            sheetsUrl = article.sheetsUrl,
            cachedAtMillis = article.cachedAtMillis
        )
    }
}
