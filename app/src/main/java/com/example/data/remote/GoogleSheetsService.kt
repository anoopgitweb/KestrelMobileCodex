package com.example.data.remote

import com.example.data.model.NewsArticle
import com.example.util.IstTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SaveToSheetsResult(
    val isSuccess: Boolean,
    val spreadsheetId: String,
    val spreadsheetUrl: String,
    val message: String
)

class GoogleSheetsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val OAUTH_CLIENT_ID = "928892794535-631vhao4qs0pq6re22jpvjalq5eprtda.apps.googleusercontent.com"
        const val SCOPE_SPREADSHEETS = "https://www.googleapis.com/auth/spreadsheets"
        const val DEFAULT_SPREADSHEET_TITLE = "Enterprise News & Benchmarks Tracker (IST)"
        const val DEFAULT_RANGE = "Sheet1!A:G"
    }

    fun buildOAuthConsentUrl(): String {
        val encodedScope = URLEncoder.encode(SCOPE_SPREADSHEETS, "UTF-8")
        val encodedRedirect = URLEncoder.encode("https://localhost", "UTF-8")
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=$OAUTH_CLIENT_ID" +
            "&redirect_uri=$encodedRedirect" +
            "&response_type=token" +
            "&scope=$encodedScope" +
            "&prompt=consent"
    }

    fun getSpreadsheetWebUrl(spreadsheetId: String): String {
        return "https://docs.google.com/spreadsheets/d/$spreadsheetId/edit"
    }

    fun formatTsvForClipboard(
        article: NewsArticle,
        sheetCategory: String = "AI News"
    ): String {
        val savedAtIst = IstTimeUtil.formatToIstFull(System.currentTimeMillis())
        val cleanTitle = article.title.replace("\t", " ").replace("\n", " ")
        val cleanTopic = article.topicName.replace("\t", " ")
        val cleanSource = article.source.replace("\t", " ")
        val cleanIst = article.istFullTimestamp.replace("\t", " ")
        val cleanLink = article.link.replace("\t", " ")
        val cleanDesc = article.description.replace("\t", " ").replace("\n", " ")
        return "$cleanTitle\t$cleanTopic\t$cleanSource\t$cleanIst\t$cleanLink\t$cleanDesc\t$savedAtIst\t$sheetCategory"
    }

    suspend fun appendArticleToSheet(
        accessToken: String,
        spreadsheetId: String,
        article: NewsArticle,
        sheetTabName: String = "AI News"
    ): Result<SaveToSheetsResult> = withContext(Dispatchers.IO) {
        try {
            val range = "'$sheetTabName'!A:G"
            val savedAtIst = IstTimeUtil.formatToIstFull(System.currentTimeMillis())
            val valuesArray = JSONArray().apply {
                put(
                    JSONArray().apply {
                        put(article.title)
                        put(article.topicName)
                        put(article.source)
                        put(article.istFullTimestamp)
                        put(article.link)
                        put(article.description)
                        put(savedAtIst)
                    }
                )
            }

            val requestJson = JSONObject().apply {
                put("range", range)
                put("majorDimension", "ROWS")
                put("values", valuesArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val encodedRange = URLEncoder.encode(range, "UTF-8")
            val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$encodedRange:append?valueInputOption=USER_ENTERED"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val sheetUrl = getSpreadsheetWebUrl(spreadsheetId)
                Result.success(
                    SaveToSheetsResult(
                        isSuccess = true,
                        spreadsheetId = spreadsheetId,
                        spreadsheetUrl = sheetUrl,
                        message = "Article saved to '$sheetTabName' tab successfully!"
                    )
                )
            } else {
                val errorObj = try { JSONObject(responseBody).optJSONObject("error") } catch (e: Exception) { null }
                val errorMsg = errorObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                Result.failure(Exception("Google Sheets API error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNewSpreadsheet(
        accessToken: String,
        title: String = DEFAULT_SPREADSHEET_TITLE
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val articleHeaders = listOf(
                "Title",
                "Topic",
                "Source",
                "IST Published Date",
                "Article Link",
                "Summary",
                "Saved At (IST)"
            )

            fun makeSheetObj(tabTitle: String, headers: List<String>): JSONObject {
                val headerCells = JSONArray()
                for (h in headers) {
                    val cell = JSONObject().apply {
                        put("userEnteredValue", JSONObject().apply { put("stringValue", h) })
                    }
                    headerCells.put(cell)
                }
                val rowDataArray = JSONArray().apply {
                    put(JSONObject().apply { put("values", headerCells) })
                }
                return JSONObject().apply {
                    put("properties", JSONObject().apply { put("title", tabTitle) })
                    put("data", JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("startRow", 0)
                                put("startColumn", 0)
                                put("rowData", rowDataArray)
                            }
                        )
                    })
                }
            }

            val sheetsArray = JSONArray().apply {
                put(makeSheetObj("AI News", articleHeaders))
                put(makeSheetObj("Work Accounts", articleHeaders))
                put(makeSheetObj("Advisory Firms", articleHeaders))
                put(makeSheetObj("Fortune 500", listOf("Rank", "Company", "Ticker", "Sector", "Revenue (\$B)", "Profit (\$B)", "Market Cap (\$B)", "CEO", "Headquarters", "Key Highlight", "Updated (IST)")))
                put(makeSheetObj("LLM Rankings", listOf("Rank", "Model Name", "Provider", "Chatbot Arena Elo", "MMLU Score", "Context Window", "Cost / 1M Input", "Cost / 1M Output", "Strengths", "License", "Updated (IST)")))
            }

            val rootJson = JSONObject().apply {
                put("properties", JSONObject().apply { put("title", title) })
                put("sheets", sheetsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets")
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val respJson = JSONObject(responseBody)
                val newId = respJson.optString("spreadsheetId", "")
                if (newId.isNotEmpty()) {
                    Result.success(newId)
                } else {
                    Result.failure(Exception("Spreadsheet created but no ID returned"))
                }
            } else {
                val errorObj = try { JSONObject(responseBody).optJSONObject("error") } catch (e: Exception) { null }
                val errorMsg = errorObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                Result.failure(Exception("Failed to create spreadsheet: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendToWebhook(
        webhookUrl: String,
        article: NewsArticle,
        sheetCategory: String = "AI News"
    ): Result<SaveToSheetsResult> = withContext(Dispatchers.IO) {
        try {
            val savedAtIst = IstTimeUtil.formatToIstFull(System.currentTimeMillis())
            val payload = JSONObject().apply {
                put("category", sheetCategory)
                put("sheetTab", sheetCategory)
                put("title", article.title)
                put("topic", article.topicName)
                put("source", article.source)
                put("istTimestamp", article.istFullTimestamp)
                put("link", article.link)
                put("description", article.description)
                put("savedAtIst", savedAtIst)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful || response.code in 200..399) {
                Result.success(
                    SaveToSheetsResult(
                        isSuccess = true,
                        spreadsheetId = "",
                        spreadsheetUrl = webhookUrl,
                        message = if (responseBody.isNotBlank()) "Saved to '$sheetCategory': $responseBody" else "Article sent to Google Sheets '$sheetCategory' tab!"
                    )
                )
            } else {
                Result.failure(Exception("Webhook returned HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendDataRowsToWebhook(
        webhookUrl: String,
        sheetTab: String,
        rows: List<List<String>>
    ): Result<SaveToSheetsResult> = withContext(Dispatchers.IO) {
        try {
            val jsonRows = JSONArray()
            for (row in rows) {
                val jRow = JSONArray()
                for (cell in row) {
                    jRow.put(cell)
                }
                jsonRows.put(jRow)
            }

            val payload = JSONObject().apply {
                put("category", sheetTab)
                put("sheetTab", sheetTab)
                put("bulkRows", jsonRows)
                put("savedAtIst", IstTimeUtil.formatToIstFull(System.currentTimeMillis()))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful || response.code in 200..399) {
                Result.success(
                    SaveToSheetsResult(
                        isSuccess = true,
                        spreadsheetId = "",
                        spreadsheetUrl = webhookUrl,
                        message = "Exported ${rows.size} rows to '$sheetTab' sheet tab!"
                    )
                )
            } else {
                Result.failure(Exception("Webhook returned HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
