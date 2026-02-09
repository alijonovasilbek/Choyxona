// Bu fayl choyxona_android/app/src/main/java/uz/choyxona/app/data/repository/UserRepository.kt ga qo'shilishi kerak

package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.*

class UserRepository {

    private val api = ApiClient.users

    suspend fun getAllUsers(token: String): Result<List<UserResponse>> {
        return try {
            val response = api.getAllUsers("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(token: String, userId: Int): Result<UserResponse> {
        return try {
            val response = api.getUser("Bearer $token", userId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(
        token: String,
        fullName: String,
        phone: String,
        username: String,
        password: String,
        telegramChatId: String? = null,
        roles: List<String>
    ): Result<UserResponse> {
        return try {
            val request = UserCreateRequest(fullName, phone, username, password, telegramChatId, roles)
            val response = api.createUser("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(
        token: String,
        userId: Int,
        fullName: String? = null,
        phone: String? = null,
        telegramChatId: String? = null,
        isActive: Boolean? = null,
        roles: List<String>? = null
    ): Result<UserResponse> {
        return try {
            val request = UserUpdateRequest(fullName, phone, telegramChatId, isActive, roles)
            val response = api.updateUser("Bearer $token", userId, request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(token: String, userId: Int): Result<Unit> {
        return try {
            val response = api.deleteUser("Bearer $token", userId)

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