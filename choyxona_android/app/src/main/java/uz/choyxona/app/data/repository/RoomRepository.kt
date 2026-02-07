package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.*

class RoomRepository {

    private val api = ApiClient.rooms

    suspend fun getAllRooms(token: String, includeInactive: Boolean = false): Result<List<RoomResponse>> {
        return try {
            val response = api.getAllRooms("Bearer $token", includeInactive)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoom(token: String, roomId: Int): Result<RoomResponse> {
        return try {
            val response = api.getRoom("Bearer $token", roomId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRoom(
        token: String,
        name: String,
        description: String?,
        capacity: Int
    ): Result<RoomResponse> {
        return try {
            val request = RoomCreateRequest(name, description, capacity)
            val response = api.createRoom("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRoom(
        token: String,
        roomId: Int,
        name: String?,
        description: String?,
        capacity: Int?,
        isActive: Boolean?
    ): Result<RoomResponse> {
        return try {
            val request = RoomUpdateRequest(name, description, capacity, isActive)
            val response = api.updateRoom("Bearer $token", roomId, request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(token: String, roomId: Int): Result<Unit> {
        return try {
            val response = api.deleteRoom("Bearer $token", roomId)

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