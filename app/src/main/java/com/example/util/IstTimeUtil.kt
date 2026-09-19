package com.example.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility functions for parsing pubDate from RSS feeds and formatting into
 * Indian Standard Time (IST - Asia/Kolkata, UTC+05:30).
 */
object IstTimeUtil {

    val IST_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private val rfc822Formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss z",
        "dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    /**
     * Parses an RSS pubDate string into a Unix timestamp (epoch millis).
     */
    fun parseRssDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val trimmed = dateStr.trim()

        for (pattern in rfc822Formats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.ENGLISH)
                format.isLenient = true
                val date = format.parse(trimmed)
                if (date != null) {
                    return date.time
                }
            } catch (_: ParseException) {
                // Try next pattern
            } catch (_: Exception) {
                // Ignore and continue
            }
        }
        return System.currentTimeMillis()
    }

    /**
     * Formats epoch milliseconds into a full IST timestamp string:
     * e.g., "18 Sep 2026, 07:52 PM IST"
     */
    fun formatToIstFull(epochMillis: Long): String {
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH)
        format.timeZone = IST_TIME_ZONE
        return format.format(Date(epochMillis))
    }

    /**
     * Formats epoch milliseconds into a concise IST timestamp:
     * e.g., "18 Sep • 07:52 PM IST"
     */
    fun formatToIstCompact(epochMillis: Long): String {
        val format = SimpleDateFormat("dd MMM • hh:mm a 'IST'", Locale.ENGLISH)
        format.timeZone = IST_TIME_ZONE
        return format.format(Date(epochMillis))
    }

    /**
     * Formats epoch milliseconds into time only in IST:
     * e.g., "07:52 PM IST"
     */
    fun formatToIstTimeOnly(epochMillis: Long): String {
        val format = SimpleDateFormat("hh:mm:ss a 'IST'", Locale.ENGLISH)
        format.timeZone = IST_TIME_ZONE
        return format.format(Date(epochMillis))
    }

    /**
     * Returns a human-friendly relative time string (e.g. "12m ago", "2h ago")
     * alongside the IST note.
     */
    fun getRelativeTimeSpan(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMillis
        if (diff < 0) return "Just now"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val format = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                format.timeZone = IST_TIME_ZONE
                format.format(Date(epochMillis))
            }
        }
    }

    /**
     * Returns current time formatted in IST for live clock pill.
     */
    fun getCurrentIstTimeString(): String {
        val format = SimpleDateFormat("hh:mm a 'IST'", Locale.ENGLISH)
        format.timeZone = IST_TIME_ZONE
        return format.format(Date())
    }
}
