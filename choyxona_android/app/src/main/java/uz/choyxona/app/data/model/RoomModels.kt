package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== ROOM MODELS ====================

data class RoomResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("capacity")
    val capacity: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class RoomCreateRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("capacity")
    val capacity: Int
)

data class RoomUpdateRequest(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("capacity")
    val capacity: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)