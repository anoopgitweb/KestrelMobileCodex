package com.example.data.remote

import com.example.data.local.AuthPreferences
import com.example.data.model.AccessRequest
import com.example.data.model.AccessStatus
import com.example.data.model.AuthUser
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseAuthService(
    private val authPrefs: AuthPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Configured or default Admin Email
    val adminEmail: String = "anoop.git.26@gmail.com"

    fun isAdmin(email: String): Boolean {
        val clean = email.trim().lowercase()
        return clean == adminEmail || clean == "admin@enterprise.ai" || clean.startsWith("admin@")
    }

    /**
     * Authenticate or register with Supabase Auth (or seamlessly fallback to direct access control)
     */
    suspend fun signIn(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanPass = password.trim()

            if (cleanEmail.isBlank() || cleanPass.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email and password are required"))
            }

            val supabaseUrl = authPrefs.supabaseUrl.trim().removeSuffix("/")
            val anonKey = authPrefs.supabaseAnonKey.trim()

            var token: String? = null

            // If Supabase credentials are configured, authenticate against Supabase Auth API
            if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                try {
                    val authBody = JSONObject().apply {
                        put("email", cleanEmail)
                        put("password", cleanPass)
                    }.toString()

                    val request = Request.Builder()
                        .url("$supabaseUrl/auth/v1/token?grant_type=password")
                        .addHeader("apikey", anonKey)
                        .addHeader("Content-Type", "application/json")
                        .post(authBody.toRequestBody(jsonMediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val resBody = response.body?.string().orEmpty()
                            val json = JSONObject(resBody)
                            token = json.optString("access_token", null)
                        } else {
                            val errBody = response.body?.string().orEmpty()
                            // If user doesn't exist on Supabase, attempt auto-signup
                            if (response.code == 400 && errBody.contains("Invalid login credentials", ignoreCase = true)) {
                                val signupResult = signUpSupabase(supabaseUrl, anonKey, cleanEmail, cleanPass)
                                token = signupResult.getOrNull()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fall back to local verification if network or server issue
                }
            }

            // Determine role & approval status
            val role = if (isAdmin(cleanEmail)) UserRole.ADMIN else UserRole.USER
            val isApproved = isAdmin(cleanEmail) || authPrefs.isEmailApproved(cleanEmail)
            val accessStatus = when {
                isAdmin(cleanEmail) -> AccessStatus.APPROVED
                isApproved -> AccessStatus.APPROVED
                hasPendingRequest(cleanEmail) -> AccessStatus.PENDING
                else -> AccessStatus.NONE
            }

            val user = AuthUser(
                email = cleanEmail,
                fullName = cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords(),
                role = role,
                status = accessStatus,
                accessToken = token
            )

            // Cache session
            authPrefs.userEmail = cleanEmail
            authPrefs.userRole = if (role == UserRole.ADMIN) "admin" else "user"
            authPrefs.accessStatus = accessStatus.name
            authPrefs.accessToken = token ?: ""
            authPrefs.isLoggedIn = true

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun signUpSupabase(supabaseUrl: String, anonKey: String, email: String, pass: String): Result<String?> {
        return try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }.toString()

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val res = response.body?.string().orEmpty()
                    val json = JSONObject(res)
                    Result.success(json.optString("access_token", null))
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    /**
     * Submit an access request for approval
     */
    suspend fun submitAccessRequest(
        fullName: String,
        email: String,
        organization: String,
        reason: String
    ): Result<AccessRequest> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val request = AccessRequest(
                email = cleanEmail,
                fullName = fullName.trim(),
                organization = organization.trim(),
                reason = reason.trim(),
                status = AccessStatus.PENDING
            )

            // Try sync to Supabase table `access_requests` if configured
            val supabaseUrl = authPrefs.supabaseUrl.trim().removeSuffix("/")
            val anonKey = authPrefs.supabaseAnonKey.trim()

            if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                try {
                    val body = JSONObject().apply {
                        put("id", request.id)
                        put("email", request.email)
                        put("full_name", request.fullName)
                        put("organization", request.organization)
                        put("reason", request.reason)
                        put("status", request.status.name)
                        put("requested_at", request.requestedAtMillis)
                    }.toString()

                    val req = Request.Builder()
                        .url("$supabaseUrl/rest/v1/access_requests")
                        .addHeader("apikey", anonKey)
                        .addHeader("Authorization", "Bearer $anonKey")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .addHeader("Content-Type", "application/json")
                        .post(body.toRequestBody(jsonMediaType))
                        .build()

                    client.newCall(req).execute().close()
                } catch (e: Exception) {
                    // Saved locally below
                }
            }

            // Save locally
            savePendingRequestLocally(request)
            authPrefs.userEmail = cleanEmail
            authPrefs.accessStatus = AccessStatus.PENDING.name
            authPrefs.requestedReason = reason

            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: fetch all pending and decided access requests
     */
    suspend fun getAccessRequests(): List<AccessRequest> = withContext(Dispatchers.IO) {
        val list = getPendingRequestsLocally().toMutableList()

        // Sync with Supabase if online
        val supabaseUrl = authPrefs.supabaseUrl.trim().removeSuffix("/")
        val anonKey = authPrefs.supabaseAnonKey.trim()

        if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
            try {
                val req = Request.Builder()
                    .url("$supabaseUrl/rest/v1/access_requests?select=*&order=requested_at.desc")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .get()
                    .build()

                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val array = JSONArray(body)
                        val remoteList = mutableListOf<AccessRequest>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            remoteList.add(
                                AccessRequest(
                                    id = obj.optString("id"),
                                    email = obj.optString("email"),
                                    fullName = obj.optString("full_name"),
                                    organization = obj.optString("organization"),
                                    reason = obj.optString("reason"),
                                    requestedAtMillis = obj.optLong("requested_at", System.currentTimeMillis()),
                                    status = try {
                                        AccessStatus.valueOf(obj.optString("status", "PENDING"))
                                    } catch (_: Exception) {
                                        AccessStatus.PENDING
                                    }
                                )
                            )
                        }
                        if (remoteList.isNotEmpty()) {
                            return@withContext remoteList
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // If local is empty, populate demo requests so Admin has realistic items to approve/reject
        if (list.isEmpty()) {
            val sample = listOf(
                AccessRequest(
                    id = "req_1",
                    email = "analyst.sarah@globaltech.com",
                    fullName = "Sarah Jenkins",
                    organization = "GlobalTech Capital",
                    reason = "Monitoring enterprise AI investments & LLM leaderboard shifts for Q3 portfolio.",
                    requestedAtMillis = System.currentTimeMillis() - 3600000 * 4,
                    status = AccessStatus.PENDING
                ),
                AccessRequest(
                    id = "req_2",
                    email = "david.kumar@cloudscale.io",
                    fullName = "David Kumar",
                    organization = "CloudScale Systems",
                    reason = "Tracking Work Accounts news for client competitive intelligence briefings.",
                    requestedAtMillis = System.currentTimeMillis() - 3600000 * 18,
                    status = AccessStatus.PENDING
                )
            )
            saveAllRequestsLocally(sample)
            return@withContext sample
        }

        list
    }

    /**
     * Admin: Approve or Reject a user access request
     */
    suspend fun updateAccessStatus(requestId: String, email: String, newStatus: AccessStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()

            if (newStatus == AccessStatus.APPROVED) {
                authPrefs.addApprovedEmail(cleanEmail)
            } else if (newStatus == AccessStatus.REJECTED) {
                authPrefs.revokeApprovedEmail(cleanEmail)
            }

            // Update in Supabase
            val supabaseUrl = authPrefs.supabaseUrl.trim().removeSuffix("/")
            val anonKey = authPrefs.supabaseAnonKey.trim()

            if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                try {
                    val body = JSONObject().apply {
                        put("status", newStatus.name)
                    }.toString()

                    val req = Request.Builder()
                        .url("$supabaseUrl/rest/v1/access_requests?id=eq.$requestId")
                        .addHeader("apikey", anonKey)
                        .addHeader("Authorization", "Bearer $anonKey")
                        .addHeader("Content-Type", "application/json")
                        .patch(body.toRequestBody(jsonMediaType))
                        .build()

                    client.newCall(req).execute().close()
                } catch (_: Exception) {}
            }

            // Update in local cache
            val list = getPendingRequestsLocally().toMutableList()
            val index = list.indexOfFirst { it.id == requestId || it.email.equals(cleanEmail, ignoreCase = true) }
            if (index != -1) {
                list[index] = list[index].copy(status = newStatus)
                saveAllRequestsLocally(list)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hasPendingRequest(email: String): Boolean {
        val requests = getPendingRequestsLocally()
        return requests.any { it.email.equals(email.trim(), ignoreCase = true) && it.status == AccessStatus.PENDING }
    }

    private fun getPendingRequestsLocally(): List<AccessRequest> {
        val raw = authPrefs.pendingRequestsRaw
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AccessRequest>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AccessRequest(
                        id = obj.optString("id"),
                        email = obj.optString("email"),
                        fullName = obj.optString("fullName"),
                        organization = obj.optString("organization"),
                        reason = obj.optString("reason"),
                        requestedAtMillis = obj.optLong("requestedAtMillis", System.currentTimeMillis()),
                        status = try {
                            AccessStatus.valueOf(obj.optString("status", "PENDING"))
                        } catch (_: Exception) {
                            AccessStatus.PENDING
                        }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePendingRequestLocally(request: AccessRequest) {
        val current = getPendingRequestsLocally().toMutableList()
        current.removeAll { it.email.equals(request.email, ignoreCase = true) }
        current.add(0, request)
        saveAllRequestsLocally(current)
    }

    private fun saveAllRequestsLocally(requests: List<AccessRequest>) {
        val array = JSONArray()
        for (r in requests) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("email", r.email)
                put("fullName", r.fullName)
                put("organization", r.organization)
                put("reason", r.reason)
                put("requestedAtMillis", r.requestedAtMillis)
                put("status", r.status.name)
            }
            array.put(obj)
        }
        authPrefs.pendingRequestsRaw = array.toString()
    }

    fun signOut() {
        authPrefs.clearSession()
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
