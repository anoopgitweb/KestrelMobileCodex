package com.example.data.remote

import android.util.Xml
import com.example.data.model.NewsArticle
import com.example.util.IstTimeUtil
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.UUID

object RssNewsParser {

    fun parse(inputStream: InputStream, topicId: String, topicName: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentTag: String? = null
        var inItem = false

        var title: String? = null
        var link: String? = null
        var pubDate: String? = null
        var description: String? = null
        var source: String? = null
        var sourceUrl: String? = null
        var guid: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("item", ignoreCase = true)) {
                        inItem = true
                        title = null
                        link = null
                        pubDate = null
                        description = null
                        source = null
                        sourceUrl = null
                        guid = null
                    } else if (inItem && currentTag.equals("source", ignoreCase = true)) {
                        sourceUrl = parser.getAttributeValue(null, "url")
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inItem && currentTag != null) {
                        val text = parser.text
                        when (currentTag.lowercase()) {
                            "title" -> title = (title ?: "") + text
                            "link" -> link = (link ?: "") + text
                            "pubdate" -> pubDate = (pubDate ?: "") + text
                            "description" -> description = (description ?: "") + text
                            "source" -> source = (source ?: "") + text
                            "guid" -> guid = (guid ?: "") + text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val endTag = parser.name
                    if (endTag.equals("item", ignoreCase = true)) {
                        inItem = false
                        val cleanTitle = cleanHtml(title ?: "")
                        val (extractedTitle, extractedSource) = extractTitleAndSource(cleanTitle, source)
                        val cleanDesc = cleanHtml(description ?: "")
                        val articleLink = link?.trim() ?: guid?.trim() ?: ""

                        if (extractedTitle.isNotBlank() && articleLink.isNotBlank()) {
                            val publishedMillis = IstTimeUtil.parseRssDateToMillis(pubDate)
                            val articleId = guid?.trim()?.takeIf { it.isNotBlank() }
                                ?: articleLink.hashCode().toString()

                            articles.add(
                                NewsArticle(
                                    id = articleId,
                                    title = extractedTitle,
                                    description = cleanDesc.ifBlank { "Read the latest update regarding $topicName from $extractedSource." },
                                    link = articleLink,
                                    source = extractedSource,
                                    sourceUrl = sourceUrl,
                                    publishedAtMillis = publishedMillis,
                                    rawPublishedDate = pubDate,
                                    topicId = topicId,
                                    topicName = topicName
                                )
                            )
                        }
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        return articles
    }

    private fun extractTitleAndSource(rawTitle: String, parsedSource: String?): Pair<String, String> {
        var finalSource = parsedSource?.trim()?.takeIf { it.isNotBlank() } ?: "News"
        var finalTitle = rawTitle.trim()

        if (finalTitle.contains(" - ")) {
            val lastDashIndex = finalTitle.lastIndexOf(" - ")
            val potentialSource = finalTitle.substring(lastDashIndex + 3).trim()
            val potentialTitle = finalTitle.substring(0, lastDashIndex).trim()

            if (potentialSource.length in 2..40 && potentialTitle.length > 5) {
                if (finalSource == "News" || finalSource.isBlank()) {
                    finalSource = potentialSource
                }
                finalTitle = potentialTitle
            }
        }

        return Pair(finalTitle, finalSource)
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
