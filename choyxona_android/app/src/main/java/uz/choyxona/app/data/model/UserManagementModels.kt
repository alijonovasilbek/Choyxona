package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// User Management Models
data class UserCreate(
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

data class UserUpdate(
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