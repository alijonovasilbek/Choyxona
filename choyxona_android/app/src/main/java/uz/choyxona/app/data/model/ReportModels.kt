package uz.choyxona.app.data.model

import com.google.gson.annotations.SerializedName

// ==================== REPORT MODELS ====================

data class BookingStatsResponse(
    @SerializedName("total_bookings")
    val totalBookings: Int,
    @SerializedName("kutilmoqda_count")
    val kutilmoqdaCount: Int,
    @SerializedName("muvaffaqiyatli_count")
    val muvaffaqiyatliCount: Int,
    @SerializedName("bekor_qilindi_count")
    val bekorQilindiCount: Int,
    @SerializedName("total_revenue")
    val totalRevenue: Double,
    @SerializedName("date_from")
    val dateFrom: String? = null,
    @SerializedName("date_to")
    val dateTo: String? = null
)

data class MonthlyReportResponse(
    @SerializedName("year")
    val year: Int,
    @SerializedName("month")
    val month: Int,
    @SerializedName("total_bookings")
    val totalBookings: Int,
    @SerializedName("successful_bookings")
    val successfulBookings: Int,
    @SerializedName("cancelled_bookings")
    val cancelledBookings: Int,
    @SerializedName("pending_bookings")
    val pendingBookings: Int,
    @SerializedName("total_revenue")
    val totalRevenue: Double,
    @SerializedName("by_room")
    val byRoom: List<RoomReportData>
)

data class RoomReportData(
    @SerializedName("room_id")
    val roomId: Int,
    @SerializedName("room_name")
    val roomName: String,
    @SerializedName("total_bookings")
    val totalBookings: Int,
    @SerializedName("successful_bookings")
    val successfulBookings: Int,
    @SerializedName("revenue")
    val revenue: Double
)

data class DailyReportResponse(
    @SerializedName("date")
    val date: String,
    @SerializedName("statistics")
    val statistics: DailyStatistics,
    @SerializedName("bookings")
    val bookings: List<DailyBookingInfo>
)

data class DailyStatistics(
    @SerializedName("total_bookings")
    val totalBookings: Int,
    @SerializedName("kutilmoqda")
    val kutilmoqda: Int,
    @SerializedName("muvaffaqiyatli")
    val muvaffaqiyatli: Int,
    @SerializedName("bekor_qilindi")
    val bekorQilindi: Int,
    @SerializedName("total_revenue")
    val totalRevenue: Double
)

data class DailyBookingInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("room_name")
    val roomName: String,
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
    @SerializedName("status")
    val status: BookingStatus,
    @SerializedName("total_amount")
    val totalAmount: Double? = null
)