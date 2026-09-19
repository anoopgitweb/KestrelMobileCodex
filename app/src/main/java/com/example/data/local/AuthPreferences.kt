package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AuthPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("supabase_auth_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_USER_ROLE = "auth_user_role" // "admin" or "user"
        private const val KEY_ACCESS_STATUS = "auth_access_status" // "APPROVED", "PENDING", "REJECTED", "NONE"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REQUESTED_REASON = "auth_requested_reason"
        private const val KEY_IS_LOGGED_IN = "auth_is_logged_in"
        private const val KEY_ADMIN_APPROVED_EMAILS = "admin_approved_emails"
        private const val KEY_PENDING_REQUESTS_JSON = "admin_pending_requests"
    }

    var supabaseUrl: String
        get() = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_URL, value.trim()).apply()

    var supabaseAnonKey: String
        get() = prefs.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_ANON_KEY, value.trim()).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value.trim().lowercase()).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "user") ?: "user"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var accessStatus: String
        get() = prefs.getString(KEY_ACCESS_STATUS, "NONE") ?: "NONE"
        set(value) = prefs.edit().putString(KEY_ACCESS_STATUS, value).apply()

    var accessToken: String
        get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var requestedReason: String
        get() = prefs.getString(KEY_REQUESTED_REASON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REQUESTED_REASON, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    // Local persistent access control cache / offline approvals
    var approvedEmailsRaw: String
        get() = prefs.getString(KEY_ADMIN_APPROVED_EMAILS, "anoop.git.26@gmail.com,admin@enterprise.ai") ?: "anoop.git.26@gmail.com,admin@enterprise.ai"
        set(value) = prefs.edit().putString(KEY_ADMIN_APPROVED_EMAILS, value).apply()

    var pendingRequestsRaw: String
        get() = prefs.getString(KEY_PENDING_REQUESTS_JSON, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PENDING_REQUESTS_JSON, value).apply()

    fun isEmailApproved(email: String): Boolean {
        val list = approvedEmailsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        return list.contains(email.trim().lowercase())
    }

    fun addApprovedEmail(email: String) {
        val list = approvedEmailsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toMutableSet()
        list.add(email.trim().lowercase())
        approvedEmailsRaw = list.joinToString(",")
    }

    fun revokeApprovedEmail(email: String) {
        val list = approvedEmailsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toMutableSet()
        list.remove(email.trim().lowercase())
        approvedEmailsRaw = list.joinToString(",")
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_ROLE)
            .remove(KEY_ACCESS_STATUS)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REQUESTED_REASON)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
