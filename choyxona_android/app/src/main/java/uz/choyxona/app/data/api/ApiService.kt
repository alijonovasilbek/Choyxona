package uz.choyxona.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import uz.choyxona.app.BuildConfig
import uz.choyxona.app.data.model.CheckResponse
import uz.choyxona.app.data.model.UpcomingResponse

import uz.choyxona.app.data.model.*
import java.util.concurrent.TimeUnit

// ==================== API INTERFACES ====================

interface AuthService {
    @POST("auth/login")
    suspend fun login(
        @Body request: UserLoginRequest
    ): Response<TokenResponse>

    @POST("auth/register")
    suspend fun register(@Body request: UserRegisterRequest): Response<UserResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<UserResponse>

    @POST("auth/switch-filial")
    suspend fun switchFilial(
        @Header("Authorization") token: String,
        @Query("filial_id") filialId: Int
    ): Response<TokenResponse>

    @GET("auth/available-filials")
    suspend fun getAvailableFilials(): Response<AvailableFilialsResponse>
}


interface RoomService {
    @GET("rooms")
    suspend fun getAllRooms(
        @Header("Authorization") token: String,
        @Query("filial_id") filialId: Int? = null,
        @Query("include_inactive") includeInactive: Boolean = false
    ): Response<List<RoomResponse>>

    @GET("rooms/{room_id}")
    suspend fun getRoom(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: Int
    ): Response<RoomResponse>

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: RoomCreateRequest
    ): Response<RoomResponse>

    @PUT("rooms/{room_id}")
    suspend fun updateRoom(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: Int,
        @Body request: RoomUpdateRequest
    ): Response<RoomResponse>

    @DELETE("rooms/{room_id}")
    suspend fun deleteRoom(
        @Header("Authorization") token: String,
        @Path("room_id") roomId: Int
    ): Response<Unit>
}


interface BookingService {
    @GET("bookings")
    suspend fun getAllBookings(
        @Header("Authorization") token: String,
        @Query("booking_date") bookingDate: String? = null,
        @Query("room_id") roomId: Int? = null,
        @Query("filial_id") filialId: Int? = null,
        @Query("status_filter") statusFilter: String? = null
    ): Response<List<BookingResponse>>

    @GET("bookings/{booking_id}")
    suspend fun getBooking(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Response<BookingResponse>

    @GET("bookings/by-date/{date_value}")
    suspend fun getBookingsByDate(
        @Header("Authorization") token: String,
        @Path("date_value") date: String
    ): Response<List<BookingResponse>>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body request: BookingCreateRequest
    ): Response<JsonObject>

    @PUT("bookings/{booking_id}")
    suspend fun updateBooking(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int,
        @Body request: BookingUpdateRequest
    ): Response<BookingResponse>

    @PATCH("bookings/{booking_id}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int,
        @Body request: BookingStatusUpdateRequest
    ): Response<BookingResponse>

    @DELETE("bookings/{booking_id}")
    suspend fun deleteBooking(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Response<Unit>
}

interface UserService {
    @GET("users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<List<UserResponse>>

    @GET("users/{user_id}")
    suspend fun getUser(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Response<UserResponse>

    @POST("users")
    suspend fun createUser(
        @Header("Authorization") token: String,
        @Body request: UserCreateRequest
    ): Response<UserResponse>

    @PUT("users/{user_id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int,
        @Body request: UserUpdateRequest
    ): Response<UserResponse>

    @DELETE("users/{user_id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Response<Unit>
}

interface ReportService {
    @GET("reports/stats")
    suspend fun getStats(
        @Header("Authorization") token: String,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<BookingStatsResponse>

    @GET("reports/monthly/{year}/{month}")
    suspend fun getMonthlyReport(
        @Header("Authorization") token: String,
        @Path("year") year: Int,
        @Path("month") month: Int
    ): Response<MonthlyReportResponse>

    @GET("reports/daily/{date_value}")
    suspend fun getDailyReport(
        @Header("Authorization") token: String,
        @Path("date_value") date: String
    ): Response<DailyReportResponse>
}


interface NotificationApi {
    @GET("notifications/check")
    suspend fun checkNotifications(): CheckResponse

    @GET("notifications/upcoming")
    suspend fun getUpcomingNotifications(
        @Query("hours_before") hours: Int = 2
    ): UpcomingResponse
}

// ==================== API CLIENT ====================

object ApiClient {

    // TO'G'RILANDI: BASE_URL oxirida "/" qo'shamiz agar yo'q bo'lsa
    private val BASE_URL = if (BuildConfig.BASE_URL.endsWith("/")) {
        BuildConfig.BASE_URL
    } else {
        "${BuildConfig.BASE_URL}/"
    }


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val auth: AuthService = retrofit.create(AuthService::class.java)
    val rooms: RoomService = retrofit.create(RoomService::class.java)
    val bookings: BookingService = retrofit.create(BookingService::class.java)
    val users: UserService = retrofit.create(UserService::class.java)
    val reports: ReportService = retrofit.create(ReportService::class.java)
    val notifications: NotificationApi = retrofit.create(NotificationApi::class.java)
}



