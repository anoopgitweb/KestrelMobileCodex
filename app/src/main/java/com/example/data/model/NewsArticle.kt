package com.example.data.model

import com.example.util.IstTimeUtil

data class NewsArticle(
    val id: String,
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val sourceUrl: String? = null,
    val publishedAtMillis: Long = System.currentTimeMillis(),
    val rawPublishedDate: String? = null,
    val topicId: String,
    val topicName: String,
    val isBookmarked: Boolean = false,
    val isSavedToSheets: Boolean = false,
    val sheetsSavedAtMillis: Long = 0L,
    val sheetsUrl: String? = null,
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    val istFullTimestamp: String
        get() = IstTimeUtil.formatToIstFull(publishedAtMillis)

    val istCompactTimestamp: String
        get() = IstTimeUtil.formatToIstCompact(publishedAtMillis)

    val relativeTime: String
        get() = IstTimeUtil.getRelativeTimeSpan(publishedAtMillis)
}
