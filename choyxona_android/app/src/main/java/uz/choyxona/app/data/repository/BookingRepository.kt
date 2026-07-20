package uz.choyxona.app.data.repository

import com.google.gson.Gson
import uz.choyxona.app.data.api.RetrofitClient
import uz.choyxona.app.data.api.ApiErrorMapper
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
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Bitta bronni olish
    suspend fun getBooking(token: String, bookingId: Int): Result<BookingResponse> {
        return try {
            val response = api.getBooking("Bearer $token", bookingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
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
                val backendMessage = response.errorBody()?.string()
                Result.failure(
                    Exception(localizeBookingError(backendMessage, "Bron yaratib bo'lmadi"))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
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
                val backendMessage = response.errorBody()?.string()
                Result.failure(
                    Exception(localizeBookingError(backendMessage, "Bron yangilanmadi"))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
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
                val backendMessage = response.errorBody()?.string()
                Result.failure(
                    Exception(localizeBookingError(backendMessage, "Bron holatini yangilab bo'lmadi"))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Bronni o'chirish
    suspend fun deleteBooking(token: String, bookingId: Int): Result<Unit> {
        return try {
            val response = api.deleteBooking("Bearer $token", bookingId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val backendMessage = response.errorBody()?.string()
                Result.failure(
                    Exception(localizeBookingError(backendMessage, "Bron o'chirib bo'lmadi"))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    private fun localizeBookingError(rawMessage: String?, fallbackUz: String): String {
        val message = extractServerMessage(rawMessage).ifBlank { fallbackUz }
        val normalized = message.lowercase()

        return when {
            normalized.contains("room is already") ||
                normalized.contains("already booked") ||
                normalized.contains("already reserved") ||
                normalized.contains("already occupied") -> "Bu xonaga bron qilingan"
            else -> message
        }
    }

    private fun extractServerMessage(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) return ""
        val trimmed = rawMessage.trim()

        return runCatching {
            val asMap = gson.fromJson(trimmed, Map::class.java)
            val detail = asMap["detail"]?.toString()
            val message = asMap["message"]?.toString()
            val error = asMap["error"]?.toString()
            detail ?: message ?: error ?: trimmed
        }.getOrElse {
            trimmed
        }
    }
}
