package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== ROOM MODELS ====================

data class RoomResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("filial_id")
    val filialId: Int,
    @SerializedName("filial_name")
    val filialName: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("capacity")
    val capacity: Int = 0,
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
    @SerializedName("filial_id")
    val filialId: Int,
    @SerializedName("description")
    val description: String? = null
)

data class RoomUpdateRequest(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)
