package uz.choyxona.app.data.repository

import android.util.Log
import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.*

class BookingRepository {

    private val api = ApiClient.bookings

    suspend fun getAllBookings(
        token: String,
        bookingDate: String? = null,
        roomId: Int? = null,
        statusFilter: String? = null
    ): Result<List<BookingResponse>> {
        return try {
            val response = api.getAllBookings("Bearer $token", bookingDate, roomId, statusFilter)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBooking(token: String, bookingId: Int): Result<BookingResponse> {
        return try {
            val response = api.getBooking("Bearer $token", bookingId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsByDate(token: String, date: String): Result<List<BookingResponse>> {
        return try {
            val response = api.getBookingsByDate("Bearer $token", date)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBooking(
        token: String,
        roomId: Int,
        bookingDate: String,
        bookingTime: String,
        customerName: String,
        customerPhone: String,
        guestCount: Int,
        foodDescription: String,
        description: String? = null
    ): Result<BookingResponse> {
        return try {
            val request = BookingCreateRequest(
                roomId = roomId,
                bookingDate = bookingDate,
                bookingTime = bookingTime,
                customerName = customerName,
                customerPhone = customerPhone,
                guestCount = guestCount,
                foodDescription = foodDescription,
                description = description
            )
            val response = api.createBooking("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Noto'g'ri ma'lumot kiritildi"
                    401 -> "Avtorizatsiya xatosi"
                    404 -> "Xona topilmadi"
                    422 -> "Ma'lumotlar formati noto'g'ri"
                    else -> "Xatolik yuz berdi: ${response.code()}"
                }
                Log.e("BookingRepository", "Create booking failed: ${response.code()} - ${response.errorBody()?.string()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Create booking exception", e)
            Result.failure(Exception("Tarmoq xatosi: ${e.message}"))
        }
    }

    suspend fun updateBooking(
        token: String,
        bookingId: Int,
        roomId: Int? = null,
        bookingDate: String? = null,
        bookingTime: String? = null,
        customerName: String? = null,
        customerPhone: String? = null,
        guestCount: Int? = null,
        foodDescription: String? = null,
        description: String? = null
    ): Result<BookingResponse> {
        return try {
            val request = BookingUpdateRequest(
                roomId = roomId,
                bookingDate = bookingDate,
                bookingTime = bookingTime,
                customerName = customerName,
                customerPhone = customerPhone,
                guestCount = guestCount,
                foodDescription = foodDescription,
                description = description
            )
            val response = api.updateBooking("Bearer $token", bookingId, request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
                val errorMsg = when (response.code()) {
                    400 -> "Noto'g'ri ma'lumot"
                    404 -> "Bron topilmadi"
                    422 -> "Ma'lumot formati noto'g'ri"
                    else -> "Xatolik: ${response.code()}"
                }
                Log.e("BookingRepository", "Status update failed: ${response.code()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Status update exception", e)
            Result.failure(Exception("Tarmoq xatosi"))
        }
    }

    suspend fun deleteBooking(token: String, bookingId: Int): Result<Unit> {
        return try {
            val response = api.deleteBooking("Bearer $token", bookingId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}