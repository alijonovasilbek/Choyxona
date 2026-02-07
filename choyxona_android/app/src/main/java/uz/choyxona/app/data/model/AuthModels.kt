package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== AUTH MODELS ====================

data class UserLoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String = "bearer"
)

data class UserResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("telegram_chat_id")
    val telegramChatId: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("roles")
    val roles: List<String>,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class UserRegisterRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

// ==================== USER MANAGEMENT MODELS ====================

data class UserCreateRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("telegram_chat_id")
    val telegramChatId: String? = null,
    @SerializedName("roles")
    val roles: List<String>
)

data class UserUpdateRequest(
    @SerializedName("full_name")
    val fullName: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("telegram_chat_id")
    val telegramChatId: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("roles")
    val roles: List<String>? = null
)