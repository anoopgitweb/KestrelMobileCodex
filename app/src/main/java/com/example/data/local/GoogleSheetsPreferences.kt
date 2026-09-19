package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class GoogleSheetsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("google_sheets_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SPREADSHEET_ID = "sheets_spreadsheet_id"
        private const val KEY_ACCESS_TOKEN = "sheets_access_token"
        private const val KEY_WEBHOOK_URL = "sheets_webhook_url"
        private const val KEY_LAST_SAVED_URL = "sheets_last_saved_url"
        private const val KEY_SELECTED_WORK_ACCOUNTS = "selected_work_accounts_csv"
        private const val KEY_CUSTOM_WORK_ACCOUNTS = "custom_work_accounts_csv"
    }

    var spreadsheetId: String
        get() = prefs.getString(KEY_SPREADSHEET_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SPREADSHEET_ID, value.trim()).apply()

    var accessToken: String
        get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value.trim()).apply()

    var webhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value.trim()).apply()

    var lastSavedSpreadsheetUrl: String
        get() = prefs.getString(KEY_LAST_SAVED_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SAVED_URL, value.trim()).apply()

    var selectedWorkAccountsRaw: String
        get() = prefs.getString(KEY_SELECTED_WORK_ACCOUNTS, "Microsoft,Google / Alphabet,Amazon Web Services (AWS),NVIDIA,Salesforce,Oracle") ?: "Microsoft,Google / Alphabet,Amazon Web Services (AWS),NVIDIA,Salesforce,Oracle"
        set(value) = prefs.edit().putString(KEY_SELECTED_WORK_ACCOUNTS, value.trim()).apply()

    var customWorkAccountsRaw: String
        get() = prefs.getString(KEY_CUSTOM_WORK_ACCOUNTS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_WORK_ACCOUNTS, value.trim()).apply()

    val isConfigured: Boolean
        get() = accessToken.isNotBlank() || webhookUrl.isNotBlank() || spreadsheetId.isNotBlank()
}
