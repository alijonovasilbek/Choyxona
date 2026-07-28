package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.api.ApiErrorMapper
import uz.choyxona.app.data.model.SaboyCreateRequest
import uz.choyxona.app.data.model.SaboyResponse
import uz.choyxona.app.data.model.SaboyUpdateRequest

class SaboyRepository {

    private val api = ApiClient.saboys

    suspend fun getSaboys(
        token: String,
        filialId: Int? = null,
        saboyDate: String? = null
    ): Result<List<SaboyResponse>> {
        return try {
            val response = api.getSaboys("Bearer $token", filialId, saboyDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun createSaboy(
        token: String,
        filialId: Int,
        saboyDate: String,
        saboyTime: String,
        description: String
    ): Result<SaboyResponse> {
        return try {
            val response = api.createSaboy(
                "Bearer $token",
                SaboyCreateRequest(
                    filialId = filialId,
                    saboyDate = saboyDate,
                    saboyTime = saboyTime,
                    description = description
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun updateSaboy(
        token: String,
        saboyId: Int,
        saboyDate: String? = null,
        saboyTime: String? = null,
        description: String? = null
    ): Result<SaboyResponse> {
        return try {
            val response = api.updateSaboy(
                "Bearer $token",
                saboyId,
                SaboyUpdateRequest(
                    saboyDate = saboyDate,
                    saboyTime = saboyTime,
                    description = description
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun deleteSaboy(token: String, saboyId: Int): Result<Unit> {
        return try {
            val response = api.deleteSaboy("Bearer $token", saboyId)

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
