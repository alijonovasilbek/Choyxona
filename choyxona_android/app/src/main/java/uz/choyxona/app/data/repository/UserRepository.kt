package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.RetrofitClient
import uz.choyxona.app.data.api.ApiErrorMapper
import uz.choyxona.app.data.model.*

class UserRepository {
    private val api = RetrofitClient.users

    // Foydalanuvchilarni olish
    suspend fun getUsers(token: String): Result<List<UserResponse>> {
        return getAllUsers(token)
    }

    suspend fun getAllUsers(token: String): Result<List<UserResponse>> {
        return try {
            val response = api.getAllUsers("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Bitta foydalanuvchini olish
    suspend fun getUser(token: String, userId: Int): Result<UserResponse> {
        return try {
            val response = api.getUser("Bearer $token", userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Foydalanuvchi yaratish - YANGI API (filialId bilan, telegramChatId yo'q)
    suspend fun createUser(
        token: String,
        fullName: String,
        phone: String,
        username: String,
        password: String,
        filialId: Int? = null,
        roles: List<String>
    ): Result<UserResponse> {
        return try {
            val request = UserCreateRequest(
                fullName = fullName,
                phone = phone,
                username = username,
                password = password,
                filialId = filialId,
                roles = roles
            )
            val response = api.createUser("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Foydalanuvchini yangilash - YANGI API (filialId bilan, telegramChatId yo'q)
    suspend fun updateUser(
        token: String,
        userId: Int,
        fullName: String? = null,
        phone: String? = null,
        filialId: Int? = null,
        isActive: Boolean? = null,
        roles: List<String>? = null
    ): Result<UserResponse> {
        return try {
            val request = UserUpdateRequest(
                fullName = fullName,
                phone = phone,
                filialId = filialId,
                isActive = isActive,
                roles = roles
            )
            val response = api.updateUser("Bearer $token", userId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    // Foydalanuvchini o'chirish
    suspend fun deleteUser(token: String, userId: Int): Result<Unit> {
        return try {
            val response = api.deleteUser("Bearer $token", userId)
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
