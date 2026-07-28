package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== SABOY MODELS ====================

data class SaboyResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("filial_id")
    val filialId: Int,
    @SerializedName("filial_name")
    val filialName: String? = null,
    @SerializedName("saboy_date")
    val saboyDate: String,
    @SerializedName("saboy_time")
    val saboyTime: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("created_by")
    val createdBy: Int,
    @SerializedName("created_by_name")
    val createdByName: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class SaboyCreateRequest(
    @SerializedName("filial_id")
    val filialId: Int,
    @SerializedName("saboy_date")
    val saboyDate: String,
    @SerializedName("saboy_time")
    val saboyTime: String,
    @SerializedName("description")
    val description: String
)

data class SaboyUpdateRequest(
    @SerializedName("saboy_date")
    val saboyDate: String? = null,
    @SerializedName("saboy_time")
    val saboyTime: String? = null,
    @SerializedName("description")
    val description: String? = null
)
