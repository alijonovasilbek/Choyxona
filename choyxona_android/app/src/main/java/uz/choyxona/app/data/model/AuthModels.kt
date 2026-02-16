package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== AUTH MODELS ====================

data class UserLoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class FilialInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)

data class AvailableFilialsResponse(
    @SerializedName("filials")
    val filials: List<FilialInfo>
)

data class UserInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("roles")
    val roles: List<String>,
    @SerializedName("user_filial_id")
    val userFilialId: Int? = null,
    @SerializedName("active_filial_id")
    val activeFilialId: Int? = null,
    @SerializedName("available_filials")
    val availableFilials: List<FilialInfo>,
    @SerializedName("is_oshpaz")
    val isOshpaz: Boolean
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String = "bearer",
    @SerializedName("user_info")
    val userInfo: UserInfo
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
    @SerializedName("filial_id")
    val filialId: Int? = null,
    @SerializedName("filial_name")
    val filialName: String? = null,
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
    @SerializedName("filial_id")
    val filialId: Int? = null,
    @SerializedName("roles")
    val roles: List<String>
)

data class UserUpdateRequest(
    @SerializedName("full_name")
    val fullName: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("filial_id")
    val filialId: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("roles")
    val roles: List<String>? = null
)
