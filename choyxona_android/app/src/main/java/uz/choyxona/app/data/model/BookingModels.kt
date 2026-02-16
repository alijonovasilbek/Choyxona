package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== BOOKING MODELS ====================

data class BookingResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("room_id")
    val roomId: Int,
    @SerializedName("room_name")
    val roomName: String,
    @SerializedName("filial_id")
    val filialId: Int,
    @SerializedName("filial_name")
    val filialName: String? = null,
    @SerializedName("booking_date")
    val bookingDate: String,
    @SerializedName("booking_time")
    val bookingTime: String = "00:00",
    @SerializedName("customer_name")
    val customerName: String = "",
    @SerializedName("customer_phone")
    val customerPhone: String = "",
    @SerializedName("guest_count")
    val guestCount: Int = 0,
    @SerializedName("food_description")
    val foodDescription: String = "",
    @SerializedName("total_amount")
    val totalAmount: Double? = null,
    @SerializedName("cancellation_reason")
    val cancellationReason: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("status")
    val status: BookingStatus,
    @SerializedName("created_by")
    val createdBy: Int,
    @SerializedName("created_by_name")
    val createdByName: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class BookingCreateRequest(
    @SerializedName("room_id")
    val roomId: Int,
    @SerializedName("booking_date")
    val bookingDate: String,
    @SerializedName("description")
    val description: String? = null
)

data class BookingUpdateRequest(
    @SerializedName("room_id")
    val roomId: Int? = null,
    @SerializedName("booking_date")
    val bookingDate: String? = null,
    @SerializedName("description")
    val description: String? = null
)

data class BookingStatusUpdateRequest(
    @SerializedName("status")
    val status: BookingStatus,
    @SerializedName("total_amount")
    val totalAmount: Double? = null,
    @SerializedName("cancellation_reason")
    val cancellationReason: String? = null
)

enum class BookingStatus {
    @SerializedName("KUTILMOQDA")
    KUTILMOQDA,

    @SerializedName("MUVAFFAQIYATLI")
    MUVAFFAQIYATLI,

    @SerializedName("BEKOR_QILINDI")
    BEKOR_QILINDI
}

fun BookingStatus.getDisplayName(): String {
    return when (this) {
        BookingStatus.KUTILMOQDA -> "Kutilmoqda"
        BookingStatus.MUVAFFAQIYATLI -> "Muvaffaqiyatli"
        BookingStatus.BEKOR_QILINDI -> "Bekor qilindi"
    }
}

fun BookingStatus.getColorCode(): String {
    return when (this) {
        BookingStatus.KUTILMOQDA -> "#F59E0B" // Orange
        BookingStatus.MUVAFFAQIYATLI -> "#10B981" // Green
        BookingStatus.BEKOR_QILINDI -> "#EF4444" // Red
    }
}
