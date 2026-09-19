package com.example.data.model

data class AccessRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val email: String,
    val fullName: String,
    val organization: String,
    val reason: String,
    val requestedAtMillis: Long = System.currentTimeMillis(),
    val status: AccessStatus = AccessStatus.PENDING,
    val role: UserRole = UserRole.USER
)

enum class AccessStatus(val title: String) {
    APPROVED("Approved"),
    PENDING("Pending Approval"),
    REJECTED("Access Denied"),
    NONE("No Request")
}

enum class UserRole(val title: String) {
    ADMIN("Administrator"),
    USER("Standard User")
}

data class AuthUser(
    val email: String,
    val fullName: String = "",
    val role: UserRole = UserRole.USER,
    val status: AccessStatus = AccessStatus.NONE,
    val accessToken: String? = null
)
