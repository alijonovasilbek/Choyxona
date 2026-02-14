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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.choyxona.app.data.local.TokenManager
import uz.choyxona.app.data.repository.BookingRepository
import uz.choyxona.app.data.repository.ReportRepository
import uz.choyxona.app.data.repository.RoomRepository
import uz.choyxona.app.data.repository.UserRepository
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
                onNavigateToUsers = {
                    navController.navigate("users")
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
                    navController.navigate("create_booking")
                },
                onEditBooking = { booking ->
                    navController.navigate("edit_booking/${booking.id}")
                },
                onDeleteBooking = { booking ->
                    scope.launch {
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val repository = BookingRepository()
                            val result = repository.deleteBooking(token, booking.id)
                            if (result.isSuccess) {
                                mainViewModel.loadBookings()
                            }
                        }
                    }
                },
                onUpdateStatus = { booking, status, totalAmount, cancellationReason ->
                    scope.launch {
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val repository = BookingRepository()
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

        // Bron yaratish
        composable("create_booking") {
            var isLoading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            CreateBookingScreen(
                rooms = mainUiState.rooms,
                onCreateBooking = { roomId, date, time, customerName, customerPhone, guestCount, foodDescription, description ->
                    scope.launch {
                        isLoading = true
                        error = null
                        val repository = BookingRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.createBooking(
                                token = token,
                                roomId = roomId,
                                bookingDate = date,
                                bookingTime = time,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                guestCount = guestCount,
                                foodDescription = foodDescription,
                                description = description
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                mainViewModel.loadData()  // TO'G'RILANDI: Barcha ma'lumotni qayta yuklash
                                navController.popBackStack()
                            } else {
                                error = result.exceptionOrNull()?.message ?: "Bron yaratib bo'lmadi"
                            }
                        } else {
                            isLoading = false
                            error = "Avtorizatsiya xatosi"
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isLoading = isLoading,
                error = error
            )
        }

        // Bron tahrirlash
        composable(
            route = "edit_booking/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: return@composable
            val booking = mainUiState.bookings.find { it.id == bookingId }

            if (booking != null) {
                var isLoading by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }

                EditBookingScreen(
                    booking = booking,
                    rooms = mainUiState.rooms,
                    onUpdateBooking = { roomId, date, time, customerName, customerPhone, guestCount, foodDescription, description ->
                        scope.launch {
                            isLoading = true
                            error = null
                            val repository = BookingRepository()
                            val token = tokenManager.accessToken.first()
                            if (token != null) {
                                val result = repository.updateBooking(
                                    token = token,
                                    bookingId = bookingId,
                                    roomId = roomId,
                                    bookingDate = date,
                                    bookingTime = time,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    guestCount = guestCount,
                                    foodDescription = foodDescription,
                                    description = description
                                )
                                isLoading = false
                                if (result.isSuccess) {
                                    mainViewModel.loadBookings()
                                    navController.popBackStack()
                                } else {
                                    error = result.exceptionOrNull()?.message ?: "Bron yangilab bo'lmadi"
                                }
                            } else {
                                isLoading = false
                                error = "Avtorizatsiya xatosi"
                            }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    isLoading = isLoading,
                    error = error
                )
            }
        }

        // Xonalar
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
                    navController.navigate("create_room")
                },
                onEditRoom = { room ->
                    navController.navigate("edit_room/${room.id}")
                },
                onDeleteRoom = { room ->
                    mainViewModel.deleteRoom(room.id)
                }
            )
        }

        // Xona yaratish
        composable("create_room") {
            CreateRoomScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCreateRoom = { name, description, capacity ->
                    scope.launch {
                        val repository = RoomRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.createRoom(token, name, description, capacity)
                            if (result.isSuccess) {
                                mainViewModel.loadRooms()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            )
        }

        // Xona tahrirlash
        composable(
            route = "edit_room/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.IntType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getInt("roomId") ?: return@composable
            val room = mainUiState.rooms.find { it.id == roomId }

            if (room != null) {
                EditRoomScreen(
                    room = room,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onUpdateRoom = { id, name, description, capacity, isActive ->
                        scope.launch {
                            val repository = RoomRepository()
                            val token = tokenManager.accessToken.first()
                            if (token != null) {
                                val result = repository.updateRoom(
                                    token = token,
                                    roomId = id,
                                    name = name,
                                    description = description,
                                    capacity = capacity,
                                    isActive = isActive
                                )
                                if (result.isSuccess) {
                                    mainViewModel.loadRooms()
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                )
            }
        }

        // Hisobotlar
        composable("reports") {
            ReportsScreen(
                isLoading = mainUiState.isLoading,
                stats = mainUiState.reportStats,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Foydalanuvchilar (faqat superadmin uchun)
        composable("users") {
            UsersScreen(
                users = mainUiState.users,
                currentUserId = uiState.currentUser?.id ?: 0,
                isLoading = mainUiState.isLoading,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCreateUser = {
                    navController.navigate("create_user")
                },
                onEditUser = { user ->
                    navController.navigate("edit_user/${user.id}")
                },
                onDeleteUser = { user ->
                    scope.launch {
                        val repository = UserRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.deleteUser(token, user.id)
                            if (result.isSuccess) {
                                mainViewModel.loadUsers()
                            }
                        }
                    }
                }
            )
        }

        // Foydalanuvchi yaratish
        composable("create_user") {
            CreateUserScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCreateUser = { fullName, phone, username, password, telegramChatId, roles ->
                    scope.launch {
                        val repository = UserRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.createUser(
                                token = token,
                                fullName = fullName,
                                phone = phone,
                                username = username,
                                password = password,
                                telegramChatId = telegramChatId,
                                roles = roles
                            )
                            if (result.isSuccess) {
                                mainViewModel.loadUsers()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            )
        }

        // Foydalanuvchi tahrirlash
        composable(
            route = "edit_user/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
            val user = mainUiState.users.find { it.id == userId }

            if (user != null) {
                EditUserScreen(
                    user = user,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onUpdateUser = { id, fullName, phone, telegramChatId, isActive, roles ->
                        scope.launch {
                            val repository = UserRepository()
                            val token = tokenManager.accessToken.first()
                            if (token != null) {
                                val result = repository.updateUser(
                                    token = token,
                                    userId = id,
                                    fullName = fullName,
                                    phone = phone,
                                    telegramChatId = telegramChatId,
                                    isActive = isActive,
                                    roles = roles
                                )
                                if (result.isSuccess) {
                                    mainViewModel.loadUsers()
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}