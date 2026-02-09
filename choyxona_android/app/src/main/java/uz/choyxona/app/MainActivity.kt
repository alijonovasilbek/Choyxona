package uz.choyxona.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.choyxona.app.data.local.TokenManager
import uz.choyxona.app.data.repository.BookingRepository
import uz.choyxona.app.data.repository.ReportRepository
import uz.choyxona.app.data.repository.RoomRepository
import uz.choyxona.app.ui.screen.*
import uz.choyxona.app.ui.theme.ChoyxonaTheme
import uz.choyxona.app.ui.viewmodel.AuthViewModel
import uz.choyxona.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(applicationContext)

        setContent {
            ChoyxonaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChoyxonaApp(tokenManager)
                }
            }
        }
    }
}

@Composable
fun ChoyxonaApp(
    tokenManager: TokenManager
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = viewModel {
        AuthViewModel(tokenManager = tokenManager)
    }
    val mainViewModel: MainViewModel = viewModel {
        MainViewModel(
            tokenManager = tokenManager,
            bookingRepository = BookingRepository(),
            roomRepository = RoomRepository(),
            reportRepository = ReportRepository()
        )
    }

    val uiState by authViewModel.uiState.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()

    // Navigation
    NavHost(
        navController = navController,
        startDestination = if (uiState.isLoggedIn) "dashboard" else "login"
    ) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    mainViewModel.loadData()
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                currentUser = uiState.currentUser,
                stats = mainUiState.dashboardStats,
                onNavigateToBookings = {
                    navController.navigate("weekly_bookings")
                },
                onNavigateToRooms = {
                    navController.navigate("rooms")
                },
                onNavigateToReports = {
                    navController.navigate("reports")
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Haftalik kalendar ko'rinishi
        composable("weekly_bookings") {
            WeeklyBookingsScreen(
                bookings = mainUiState.bookings,
                isLoading = mainUiState.isLoading,
                onRefresh = {
                    mainViewModel.loadBookings()
                },
                onBookingClick = { booking ->
                    // TODO: Navigate to booking details if needed
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCreateBooking = {
                    // TODO: Navigate to create booking
                },
                onEditBooking = { booking ->
                    // TODO: Navigate to edit booking screen
                },
                onDeleteBooking = { booking ->
                    scope.launch {
                        val repository = BookingRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.deleteBooking(token, booking.id)
                            if (result.isSuccess) {
                                mainViewModel.loadBookings()
                            }
                        }
                    }
                },
                onUpdateStatus = { booking, status, totalAmount, cancellationReason ->
                    scope.launch {
                        val repository = BookingRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.updateBookingStatus(
                                token = token,
                                bookingId = booking.id,
                                status = status,
                                totalAmount = totalAmount,
                                cancellationReason = cancellationReason
                            )
                            if (result.isSuccess) {
                                mainViewModel.loadBookings()
                            }
                        }
                    }
                }
            )
        }

        // Eski ro'yxat ko'rinishi (kerak bo'lsa)
        composable("bookings") {
            BookingsScreen(
                bookings = mainUiState.bookings,
                isLoading = mainUiState.isLoading,
                onRefresh = {
                    mainViewModel.loadBookings()
                },
                onBookingClick = { booking ->
                    // TODO: Navigate to booking details
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCreateBooking = {
                    // TODO: Navigate to create booking
                }
            )
        }

        composable("rooms") {
            RoomsScreen(
                rooms = mainUiState.rooms,
                isLoading = mainUiState.isLoading,
                onRefresh = {
                    mainViewModel.loadRooms()
                },
                onRoomClick = { room ->
                    // TODO: Navigate to room details
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCreateRoom = {
                    // TODO: Navigate to create room
                }
            )
        }

        composable("reports") {
            ReportsScreen(
                isLoading = mainUiState.isLoading,
                stats = mainUiState.reportStats,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}