package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.RetrofitClient
import uz.choyxona.app.data.api.ApiErrorMapper
import uz.choyxona.app.data.model.*

class RoomRepository {
    private val api = RetrofitClient.rooms

    private fun errorMessage(response: retrofit2.Response<*>, fallback: String): String {
        return try {
            val body = response.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                val detail = org.json.JSONObject(body).optString("detail")
                if (detail.isNotBlank()) detail else "$fallback (HTTP ${response.code()})"
            } else {
                "$fallback (HTTP ${response.code()})"
            }
        } catch (e: Exception) {
            "$fallback (HTTP ${response.code()})"
        }
    }

    // Xonalarni olish
    suspend fun getRooms(token: String): Result<List<RoomResponse>> {
        return getAllRooms(token)
    }

    suspend fun getAllRooms(
        token: String,
        includeInactive: Boolean = false,
        filialId: Int? = null
    ): Result<List<RoomResponse>> {
        return try {
            val response = api.getAllRooms("Bearer $token", filialId, includeInactive)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Bitta xonani olish
    suspend fun getRoom(token: String, roomId: Int): Result<RoomResponse> {
        return try {
            val response = api.getRoom("Bearer $token", roomId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Xona yaratish - YANGI API (filialId bilan, capacity yo'q)
    suspend fun createRoom(
        token: String,
        name: String,
        description: String?,
        filialId: Int
    ): Result<RoomResponse> {
        return try {
            val request = RoomCreateRequest(
                name = name,
                description = description,
                filialId = filialId
            )
            val response = api.createRoom("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Xonani yangilash - YANGI API (capacity yo'q)
    suspend fun updateRoom(
        token: String,
        roomId: Int,
        name: String? = null,
        description: String? = null,
        isActive: Boolean? = null
    ): Result<RoomResponse> {
        return try {
            val request = RoomUpdateRequest(
                name = name,
                description = description,
                isActive = isActive
            )
            val response = api.updateRoom("Bearer $token", roomId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Xonani o'chirish
    suspend fun deleteRoom(token: String, roomId: Int): Result<Unit> {
        return try {
            val response = api.deleteRoom("Bearer $token", roomId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }
}
