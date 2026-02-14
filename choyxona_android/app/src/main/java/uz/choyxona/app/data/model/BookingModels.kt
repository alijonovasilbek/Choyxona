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
    @SerializedName("booking_date")
    val bookingDate: String,
    @SerializedName("booking_time")
    val bookingTime: String,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("customer_phone")
    val customerPhone: String,
    @SerializedName("guest_count")
    val guestCount: Int,
    @SerializedName("food_description")
    val foodDescription: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("status")
    val status: BookingStatus,
    @SerializedName("total_amount")
    val totalAmount: Double? = null,
    @SerializedName("cancellation_reason")
    val cancellationReason: String? = null,
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
    @SerializedName("booking_time")
    val bookingTime: String,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("customer_phone")
    val customerPhone: String,
    @SerializedName("guest_count")
    val guestCount: Int,
    @SerializedName("food_description")
    val foodDescription: String,
    @SerializedName("description")
    val description: String? = null
)

data class BookingUpdateRequest(
    @SerializedName("room_id")
    val roomId: Int? = null,
    @SerializedName("booking_date")
    val bookingDate: String? = null,
    @SerializedName("booking_time")
    val bookingTime: String? = null,
    @SerializedName("customer_name")
    val customerName: String? = null,
    @SerializedName("customer_phone")
    val customerPhone: String? = null,
    @SerializedName("guest_count")
    val guestCount: Int? = null,
    @SerializedName("food_description")
    val foodDescription: String? = null,
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
    PENDING,

    @SerializedName("MUVAFFAQIYATLI")
    SUCCESSFUL,

    @SerializedName("BEKOR_QILINDI")
    CANCELLED
}

fun BookingStatus.getDisplayName(): String {
    return when (this) {
        BookingStatus.PENDING -> "Kutilmoqda"
        BookingStatus.SUCCESSFUL -> "Muvaffaqiyatli"
        BookingStatus.CANCELLED -> "Bekor qilindi"
    }
}

fun BookingStatus.getColorCode(): String {
    return when (this) {
        BookingStatus.PENDING -> "#F59E0B" // Orange
        BookingStatus.SUCCESSFUL -> "#10B981" // Green
        BookingStatus.CANCELLED -> "#EF4444" // Red
    }
}