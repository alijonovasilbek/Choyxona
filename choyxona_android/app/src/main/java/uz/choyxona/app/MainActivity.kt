package uz.choyxona.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import uz.choyxona.app.data.local.SettingsManager
import uz.choyxona.app.data.local.TokenManager
import uz.choyxona.app.data.model.FilialInfo
import uz.choyxona.app.data.repository.AuthRepository
import uz.choyxona.app.data.repository.BookingRepository
import uz.choyxona.app.data.repository.ReportRepository
import uz.choyxona.app.data.repository.RoomRepository
import uz.choyxona.app.data.repository.UserRepository
import uz.choyxona.app.ui.screen.*
import uz.choyxona.app.ui.theme.ChoyxonaTheme
import uz.choyxona.app.ui.theme.ThemeMode
import uz.choyxona.app.ui.viewmodel.AuthViewModel
import uz.choyxona.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)

        setContent {
            val themeMode by settingsManager.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            ChoyxonaTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChoyxonaApp(tokenManager, settingsManager, themeMode)
                }
            }
        }
    }
}

@Composable
fun ChoyxonaApp(
    tokenManager: TokenManager,
    settingsManager: SettingsManager,
    themeMode: ThemeMode
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
    val activeFilialId = uiState.currentUser?.filialId ?: uiState.userInfo?.activeFilialId
    // Every role may switch filial; oshpaz just starts in their assigned one.
    val canSwitchFilial = uiState.currentUser != null

    LaunchedEffect(activeFilialId) {
        mainViewModel.setActiveFilial(activeFilialId)
    }

    // Header switcher needs the filial list even on a restored session.
    LaunchedEffect(uiState.isLoggedIn, uiState.currentUser?.id) {
        if (uiState.isLoggedIn) authViewModel.ensureAvailableFilialsLoaded()
    }

    // Several people book at the same time, so the app polls while it is in
    // the foreground and re-reads immediately when it comes back from the
    // background. Without this a user only saw other people's bookings after
    // Android eventually killed and cold-started the activity.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.isLoggedIn) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (uiState.isLoggedIn) mainViewModel.startAutoRefresh()
                Lifecycle.Event.ON_STOP -> mainViewModel.stopAutoRefresh()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // ON_START already fired before login, so logging in has to start the
        // poll itself instead of waiting for the next background/resume cycle.
        if (uiState.isLoggedIn && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mainViewModel.startAutoRefresh()
        } else if (!uiState.isLoggedIn) {
            mainViewModel.stopAutoRefresh()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mainViewModel.stopAutoRefresh()
        }
    }

    // Navigation
    NavHost(
        navController = navController,
        startDestination = if (uiState.isLoggedIn) {
            if (uiState.needsFilialSelection) "filial_selection" else "dashboard"
        } else "login",
        enterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 6 }
        },
        exitTransition = {
            fadeOut(tween(220))
        },
        popEnterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(280)) { -it / 6 }
        },
        popExitTransition = {
            fadeOut(tween(220)) + slideOutHorizontally(tween(280)) { it / 6 }
        }
    ) {
        // ==================== LOGIN ====================
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    val latestState = authViewModel.uiState.value
                    // Check if filial selection needed
                    if (latestState.needsFilialSelection) {
                        navController.navigate("filial_selection") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        mainViewModel.loadData()
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ==================== FILIAL SELECTION ====================
        composable("filial_selection") {
            FilialSelectionScreen(
                filials = uiState.availableFilials,
                currentFilialId = activeFilialId,
                onFilialSelected = { filialId ->
                    authViewModel.selectFilial(filialId)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

            LaunchedEffect(uiState.isLoggedIn, uiState.needsFilialSelection) {
                if (uiState.isLoggedIn && !uiState.needsFilialSelection) {
                    mainViewModel.loadData()
                    navController.navigate("dashboard") {
                        popUpTo("filial_selection") { inclusive = true }
                    }
                }
            }
        }

        // ==================== DASHBOARD ====================
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
                onSwitchFilial = {
                    authViewModel.startFilialSelection()
                    navController.navigate("filial_selection")
                },
                canSwitchFilial = canSwitchFilial,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onStatsRange = { from, to ->
                    mainViewModel.loadStatsRange(from, to)
                },
                availableFilials = uiState.availableFilials,
                activeFilialId = activeFilialId,
                onFilialSelected = { filialId ->
                    authViewModel.selectFilial(filialId)
                },
                onNavigateToSaboys = {
                    navController.navigate("saboys")
                }
            )
        }

        // ==================== SABOYS ====================
        composable("saboys") {
            LaunchedEffect(Unit) {
                mainViewModel.loadSaboys()
            }

            SaboysScreen(
                saboys = mainUiState.saboys,
                isLoading = mainUiState.isLoading,
                onRefresh = { mainViewModel.loadSaboys() },
                onNavigateBack = { navController.popBackStack() },
                onCreateSaboy = { navController.navigate("create_saboy") },
                onEditSaboy = { saboy -> navController.navigate("edit_saboy/${saboy.id}") },
                onDeleteSaboy = { saboy -> mainViewModel.deleteSaboy(saboy.id) }
            )
        }

        // ==================== CREATE SABOY ====================
        composable("create_saboy") {
            var isSaving by remember { mutableStateOf(false) }
            var saveError by remember { mutableStateOf<String?>(null) }
            val filial = uiState.availableFilials.firstOrNull { it.id == activeFilialId }

            SaboyFormScreen(
                existing = null,
                filialName = filial?.name,
                isLoading = isSaving,
                error = saveError,
                onNavigateBack = { navController.popBackStack() },
                onSubmit = { date, time, description ->
                    val filialId = activeFilialId
                    if (filialId == null) {
                        saveError = "Filial tanlanmagan"
                    } else {
                        isSaving = true
                        saveError = null
                        mainViewModel.createSaboy(
                            filialId = filialId,
                            saboyDate = date,
                            saboyTime = time,
                            description = description
                        ) { error ->
                            isSaving = false
                            if (error == null) navController.popBackStack() else saveError = error
                        }
                    }
                }
            )
        }

        // ==================== EDIT SABOY ====================
        composable(
            route = "edit_saboy/{saboyId}",
            arguments = listOf(navArgument("saboyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val saboyId = backStackEntry.arguments?.getInt("saboyId") ?: return@composable
            val saboy = mainUiState.saboys.find { it.id == saboyId }

            if (saboy != null) {
                var isSaving by remember { mutableStateOf(false) }
                var saveError by remember { mutableStateOf<String?>(null) }

                SaboyFormScreen(
                    existing = saboy,
                    filialName = saboy.filialName,
                    isLoading = isSaving,
                    error = saveError,
                    onNavigateBack = { navController.popBackStack() },
                    onSubmit = { date, time, description ->
                        isSaving = true
                        saveError = null
                        mainViewModel.updateSaboy(
                            saboyId = saboyId,
                            saboyDate = date,
                            saboyTime = time,
                            description = description
                        ) { error ->
                            isSaving = false
                            if (error == null) navController.popBackStack() else saveError = error
                        }
                    }
                )
            }
        }

        // ==================== SETTINGS ====================
        composable("settings") {
            SettingsScreen(
                currentUser = uiState.currentUser,
                themeMode = themeMode,
                onThemeModeChange = { mode ->
                    scope.launch { settingsManager.setThemeMode(mode) }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSwitchFilial = {
                    authViewModel.startFilialSelection()
                    navController.navigate("filial_selection")
                },
                canSwitchFilial = canSwitchFilial,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ==================== WEEKLY BOOKINGS ====================
        composable("weekly_bookings") {
            LaunchedEffect(Unit) {
                mainViewModel.loadBookings(silent = true)
                mainViewModel.loadRooms(silent = true)
            }

            WeeklyBookingsScreen(
                bookings = mainUiState.bookings,
                rooms = mainUiState.rooms,
                isLoading = mainUiState.isLoading,
                onRefresh = {
                    mainViewModel.loadBookings()
                    mainViewModel.loadRooms(silent = true)
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
                onCreateBookingForRoomDate = { roomId, date ->
                    navController.navigate("create_booking?roomId=$roomId&date=$date")
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
                availableFilials = uiState.availableFilials,
                activeFilialId = activeFilialId,
                canSwitchFilial = canSwitchFilial,
                onFilialSelected = { filialId ->
                    authViewModel.selectFilial(filialId)
                }
            )
        }

        // ==================== CREATE BOOKING ====================
        composable(
            route = "create_booking?roomId={roomId}&date={date}",
            arguments = listOf(
                navArgument("roomId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            var isLoading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val initialRoomId = backStackEntry.arguments
                ?.getInt("roomId")
                ?.takeIf { it > 0 }
            val initialDate = backStackEntry.arguments
                ?.getString("date")
                ?.takeIf { it.isNotBlank() }

            LaunchedEffect(activeFilialId) {
                mainViewModel.loadRooms()
                mainViewModel.loadBookings(silent = true)
            }

            CreateBookingScreen(
                rooms = mainUiState.rooms,
                bookings = mainUiState.bookings,
                initialRoomId = initialRoomId,
                initialDate = initialDate,
                onCreateBooking = { roomId, date, description ->
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
                                description = description
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                mainViewModel.loadData()
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

        // ==================== EDIT BOOKING ====================
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
                    onUpdateBooking = { roomId, date, description ->
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

        // ==================== ROOMS ====================
        composable("rooms") {
            LaunchedEffect(Unit) {
                mainViewModel.loadRooms(silent = true)
            }

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

        // ==================== CREATE ROOM ====================
        composable("create_room") {
            var isCreating by remember { mutableStateOf(false) }
            var createError by remember { mutableStateOf<String?>(null) }

            CreateRoomScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                filialId = uiState.userInfo?.activeFilialId ?: uiState.currentUser?.filialId,
                isLoading = isCreating,
                error = createError,
                onCreateRoom = { name, description, filialId ->
                    scope.launch {
                        isCreating = true
                        createError = null
                        val repository = RoomRepository()
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            val result = repository.createRoom(
                                token = token,
                                name = name,
                                description = description,
                                filialId = filialId
                            )
                            isCreating = false
                            if (result.isSuccess) {
                                mainViewModel.loadRooms()
                                navController.popBackStack()
                            } else {
                                createError = result.exceptionOrNull()?.message
                                    ?: "Xona yaratishda xatolik yuz berdi"
                            }
                        } else {
                            isCreating = false
                            createError = "Sessiya tugagan, qaytadan kiring"
                        }
                    }
                }
            )
        }

        // ==================== EDIT ROOM ====================
        composable(
            route = "edit_room/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.IntType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getInt("roomId") ?: return@composable
            val room = mainUiState.rooms.find { it.id == roomId }

            if (room != null) {
                var isUpdating by remember { mutableStateOf(false) }
                var updateError by remember { mutableStateOf<String?>(null) }

                EditRoomScreen(
                    room = room,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    isLoading = isUpdating,
                    error = updateError,
                    onUpdateRoom = { id, name, description, isActive ->
                        scope.launch {
                            isUpdating = true
                            updateError = null
                            val repository = RoomRepository()
                            val token = tokenManager.accessToken.first()
                            if (token != null) {
                                val result = repository.updateRoom(
                                    token = token,
                                    roomId = id,
                                    name = name,
                                    description = description,
                                    isActive = isActive
                                )
                                isUpdating = false
                                if (result.isSuccess) {
                                    mainViewModel.loadRooms()
                                    navController.popBackStack()
                                } else {
                                    updateError = result.exceptionOrNull()?.message
                                        ?: "Xonani yangilashda xatolik yuz berdi"
                                }
                            } else {
                                isUpdating = false
                                updateError = "Sessiya tugagan, qaytadan kiring"
                            }
                        }
                    }
                )
            }
        }

        // ==================== REPORTS ====================
        composable("reports") {
            ReportsScreen(
                isLoading = mainUiState.isLoading,
                stats = mainUiState.reportStats,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ==================== USERS ====================
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

        // ==================== CREATE USER ====================
        composable("create_user") {
            var createUserFilials by remember { mutableStateOf<List<FilialInfo>>(emptyList()) }

            LaunchedEffect(
                uiState.userInfo,
                uiState.availableFilials,
                uiState.currentUser?.filialId,
                uiState.currentUser?.filialName
            ) {
                val fromAuthState = uiState.userInfo?.availableFilials?.takeIf { it.isNotEmpty() }
                    ?: uiState.availableFilials.takeIf { it.isNotEmpty() }

                createUserFilials = if (fromAuthState != null) {
                    fromAuthState
                } else {
                    val fromApi = AuthRepository().getAvailableFilials().getOrNull().orEmpty()
                    if (fromApi.isNotEmpty()) {
                        fromApi
                    } else {
                        listOfNotNull(
                            uiState.currentUser?.filialId?.let { filialId ->
                                FilialInfo(
                                    id = filialId,
                                    name = uiState.currentUser?.filialName ?: "Filial #$filialId"
                                )
                            }
                        )
                    }
                }
            }

            CreateUserScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                availableFilials = createUserFilials,
                onCreateUser = { fullName, phone, username, password, filialId, roles ->
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
                                filialId = filialId,
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

        // ==================== EDIT USER ====================
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
                    onUpdateUser = { id, fullName, phone, filialId, isActive, roles ->
                        scope.launch {
                            val repository = UserRepository()
                            val token = tokenManager.accessToken.first()
                            if (token != null) {
                                val result = repository.updateUser(
                                    token = token,
                                    userId = id,
                                    fullName = fullName,
                                    phone = phone,
                                    filialId = filialId,
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
