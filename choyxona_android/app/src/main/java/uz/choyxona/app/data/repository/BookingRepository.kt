package uz.choyxona.app.data.repository

import com.google.gson.Gson
import uz.choyxona.app.data.api.RetrofitClient
import uz.choyxona.app.data.model.*

class BookingRepository {
    private val api = RetrofitClient.bookings
    private val gson = Gson()

    // Bronlarni olish
    suspend fun getBookings(token: String): Result<List<BookingResponse>> {
        return getAllBookings(token)
    }

    suspend fun getAllBookings(
        token: String,
        bookingDate: String? = null,
        roomId: Int? = null,
        filialId: Int? = null,
        statusFilter: String? = null
    ): Result<List<BookingResponse>> {
        return try {
            val response = api.getAllBookings("Bearer $token", bookingDate, roomId, filialId, statusFilter)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch bookings: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bitta bronni olish
    suspend fun getBooking(token: String, bookingId: Int): Result<BookingResponse> {
        return try {
            val response = api.getBooking("Bearer $token", bookingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bron yaratish - YANGI API (3 ta parametr)
    suspend fun createBooking(
        token: String,
        roomId: Int,
        bookingDate: String,
        description: String? = null
    ): Result<BookingResponse> {
        return try {
            val request = BookingCreateRequest(
                roomId = roomId,
                bookingDate = bookingDate,
                description = description
            )
            val response = api.createBooking("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val parsed = gson.fromJson(response.body(), BookingResponse::class.java)
                Result.success(parsed)
            } else {
                Result.failure(
                    Exception(response.errorBody()?.string() ?: "Failed to create booking: ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bronni yangilash - YANGI API (3 ta optional parametr)
    suspend fun updateBooking(
        token: String,
        bookingId: Int,
        roomId: Int? = null,
        bookingDate: String? = null,
        description: String? = null
    ): Result<BookingResponse> {
        return try {
            val request = BookingUpdateRequest(
                roomId = roomId,
                bookingDate = bookingDate,
                description = description
            )
            val response = api.updateBooking("Bearer $token", bookingId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Status yangilash - YANGI API (faqat status)
    suspend fun updateBookingStatus(
        token: String,
        bookingId: Int,
        status: BookingStatus,
        totalAmount: Double? = null,
        cancellationReason: String? = null
    ): Result<BookingResponse> {
        return try {
            val request = BookingStatusUpdateRequest(
                status = status,
                totalAmount = totalAmount,
                cancellationReason = cancellationReason
            )
            val response = api.updateBookingStatus("Bearer $token", bookingId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update booking status: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bronni o'chirish
    suspend fun deleteBooking(token: String, bookingId: Int): Result<Unit> {
        return try {
            val response = api.deleteBooking("Bearer $token", bookingId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(response.errorBody()?.string() ?: "Failed to delete booking: ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
