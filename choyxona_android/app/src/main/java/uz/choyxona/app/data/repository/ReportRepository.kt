package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.*

class ReportRepository {

    private val api = ApiClient.reports

    suspend fun getStats(
        token: String,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Result<BookingStatsResponse> {
        return try {
            val response = api.getStats("Bearer $token", dateFrom, dateTo)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthlyReport(
        token: String,
        year: Int,
        month: Int
    ): Result<MonthlyReportResponse> {
        return try {
            val response = api.getMonthlyReport("Bearer $token", year, month)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyReport(token: String, date: String): Result<DailyReportResponse> {
        return try {
            val response = api.getDailyReport("Bearer $token", date)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}