package uz.choyxona.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.choyxona.app.data.local.TokenManager
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.RoomResponse
import uz.choyxona.app.data.model.SaboyResponse
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.data.repository.BookingRepository
import uz.choyxona.app.data.repository.ReportRepository
import uz.choyxona.app.data.repository.RoomRepository
import uz.choyxona.app.data.repository.SaboyRepository
import uz.choyxona.app.data.repository.UserRepository
import uz.choyxona.app.ui.screen.DashboardStats
import uz.choyxona.app.ui.screen.ReportStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookings: List<BookingResponse> = emptyList(),
    val rooms: List<RoomResponse> = emptyList(),
    val users: List<UserResponse> = emptyList(),
    val saboys: List<SaboyResponse> = emptyList(),
    val dashboardStats: DashboardStats? = null,
    val reportStats: ReportStats? = null
)

class MainViewModel(
    private val tokenManager: TokenManager,
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val roomRepository: RoomRepository = RoomRepository(),
    private val reportRepository: ReportRepository = ReportRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val saboyRepository: SaboyRepository = SaboyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var activeFilialId: Int? = null

    // Bookings are entered by several people at once, so the screens have to
    // pick up other users' changes without waiting for an app restart.
    private var autoRefreshJob: Job? = null

    // The dashboard lets the user pick a period; polling must not silently
    // reset it back to the default last-7-days range.
    private var statsDateFrom: String? = null
    private var statsDateTo: String? = null

    init {
        loadData()
    }

    fun setActiveFilial(filialId: Int?) {
        if (activeFilialId != filialId) {
            activeFilialId = filialId
            loadData()
        }
    }

    fun loadData() {
        loadBookings()
        loadRooms()
        loadUsers()
        loadSaboys()
        loadStats()
    }

    /**
     * Re-reads everything the screens display without touching [MainUiState.isLoading],
     * so a background poll never replaces the visible list with a spinner.
     */
    fun refreshQuietly() {
        loadBookings(silent = true)
        loadRooms(silent = true)
        loadUsers(silent = true)
        loadSaboys()
        reloadStats()
    }

    /** Poll while the app is in the foreground. Cancelled in [stopAutoRefresh]. */
    fun startAutoRefresh(intervalMillis: Long = AUTO_REFRESH_INTERVAL_MS) {
        if (autoRefreshJob?.isActive == true) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                refreshQuietly()
                delay(intervalMillis)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        stopAutoRefresh()
        super.onCleared()
    }

    fun loadSaboys() {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (token.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(error = "Not authenticated")
                return@launch
            }

            val result = saboyRepository.getSaboys(
                token = token,
                filialId = activeFilialId
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(saboys = result.getOrNull().orEmpty())
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun createSaboy(
        filialId: Int,
        saboyDate: String,
        saboyTime: String,
        description: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (token.isNullOrEmpty()) {
                onResult("Sessiya tugagan, qaytadan kiring")
                return@launch
            }

            val result = saboyRepository.createSaboy(
                token = token,
                filialId = filialId,
                saboyDate = saboyDate,
                saboyTime = saboyTime,
                description = description
            )
            if (result.isSuccess) {
                loadSaboys()
                onResult(null)
            } else {
                onResult(result.exceptionOrNull()?.message ?: "Saboy qo'shib bo'lmadi")
            }
        }
    }

    fun updateSaboy(
        saboyId: Int,
        saboyDate: String,
        saboyTime: String,
        description: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (token.isNullOrEmpty()) {
                onResult("Sessiya tugagan, qaytadan kiring")
                return@launch
            }

            val result = saboyRepository.updateSaboy(
                token = token,
                saboyId = saboyId,
                saboyDate = saboyDate,
                saboyTime = saboyTime,
                description = description
            )
            if (result.isSuccess) {
                loadSaboys()
                onResult(null)
            } else {
                onResult(result.exceptionOrNull()?.message ?: "Saboyni yangilab bo'lmadi")
            }
        }
    }

    fun deleteSaboy(saboyId: Int) {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (token.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(error = "Not authenticated")
                return@launch
            }

            val result = saboyRepository.deleteSaboy(token, saboyId)
            if (result.isSuccess) {
                loadSaboys()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadBookings(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = bookingRepository.getAllBookings(
                    token = token,
                    filialId = activeFilialId
                )
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        bookings = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    fun loadRooms(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = roomRepository.getAllRooms(
                    token = token,
                    filialId = activeFilialId
                )
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        rooms = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    fun loadUsers(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = userRepository.getAllUsers(token)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        users = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    fun loadStats() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        loadStatsRange(
            dateFrom = today.minusDays(6).format(formatter),
            dateTo = today.format(formatter)
        )
    }

    /** Re-runs the stats query for whatever period the user last selected. */
    private fun reloadStats() {
        val from = statsDateFrom
        val to = statsDateTo
        if (from != null && to != null) loadStatsRange(from, to) else loadStats()
    }

    fun loadStatsRange(dateFrom: String, dateTo: String) {
        statsDateFrom = dateFrom
        statsDateTo = dateTo

        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {

                // Load dashboard stats
                val result = reportRepository.getStats(
                    token = token,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    if (stats != null) {
                        _uiState.value = _uiState.value.copy(
                            dashboardStats = DashboardStats(
                                totalBookings = stats.totalBookings,
                                pendingCount = stats.kutilmoqdaCount,
                                successfulCount = stats.muvaffaqiyatliCount,
                                cancelledCount = stats.bekorQilindiCount,
                                totalRevenue = stats.totalRevenue
                            ),
                            reportStats = ReportStats(
                                totalBookings = stats.totalBookings,
                                pendingCount = stats.kutilmoqdaCount,
                                successfulCount = stats.muvaffaqiyatliCount,
                                cancelledCount = stats.bekorQilindiCount,
                                totalRevenue = stats.totalRevenue,
                                successfulRevenue = stats.totalRevenue
                            )
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Not authenticated"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteRoom(roomId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = roomRepository.deleteRoom(token, roomId)

                if (result.isSuccess) {
                    // O'chirgandan keyin roomlarni qayta yuklaymiz
                    loadRooms()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    companion object {
        /** Foreground poll interval. Payload is small and the API answers in ~50ms. */
        const val AUTO_REFRESH_INTERVAL_MS = 20_000L
    }
}
