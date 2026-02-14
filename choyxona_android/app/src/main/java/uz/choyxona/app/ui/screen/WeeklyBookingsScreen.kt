package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.model.getDisplayName
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun WeeklyBookingsScreen(
    bookings: List<BookingResponse>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBookingClick: (BookingResponse) -> Unit,
    onNavigateBack: () -> Unit,
    onCreateBooking: () -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit,
    onUpdateStatus: (BookingResponse, BookingStatus, Double?, String?) -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentWeekStart by remember { mutableStateOf(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedBooking by remember { mutableStateOf<BookingResponse?>(null) }
    var selectedStatus by remember { mutableStateOf<BookingStatus?>(null) }
    var totalAmount by remember { mutableStateOf("") }
    var cancellationReason by remember { mutableStateOf("") }

    // Hafta kunlari uchun bronlarni guruhlash
    val weekBookings = remember(bookings, currentWeekStart) {
        val weekDays = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
        weekDays.associateWith { date ->
            bookings.filter { booking ->
                val bookingDate = LocalDate.parse(booking.bookingDate)
                bookingDate == date
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog && selectedBooking != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedBooking = null
            },
            title = { Text("Bronni o'chirish") },
            text = {
                Text("${selectedBooking?.customerName} nomidagi bronni o'chirmoqchimisiz?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBooking?.let { onDeleteBooking(it) }
                        showDeleteDialog = false
                        selectedBooking = null
                    }
                ) {
                    Text("O'chirish", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedBooking = null
                    }
                ) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // Status Update Dialog
    if (showStatusDialog && selectedBooking != null) {
        AlertDialog(
            onDismissRequest = {
                showStatusDialog = false
                selectedBooking = null
                selectedStatus = null
                totalAmount = ""
                cancellationReason = ""
            },
            title = { Text("Bron holatini o'zgartirish") },
            text = {
                Column {
                    Text(
                        text = "Yangi holatni tanlang:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Status options
                    BookingStatus.values().forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryGreen
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(status.getDisplayName())
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Conditional fields based on status
                    when (selectedStatus) {
                        BookingStatus.SUCCESSFUL -> {
                            OutlinedTextField(
                                value = totalAmount,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        totalAmount = it
                                    }
                                },
                                label = { Text("Jami summa *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        BookingStatus.CANCELLED -> {
                            OutlinedTextField(
                                value = cancellationReason,
                                onValueChange = { cancellationReason = it },
                                label = { Text("Bekor qilish sababi *") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedStatus?.let { status ->
                            val amount = when (status) {
                                BookingStatus.SUCCESSFUL -> totalAmount.toDoubleOrNull()
                                else -> null
                            }
                            val reason = when (status) {
                                BookingStatus.CANCELLED -> cancellationReason.ifBlank { null }
                                else -> null
                            }

                            selectedBooking?.let { booking ->
                                onUpdateStatus(booking, status, amount, reason)
                            }
                        }
                        showStatusDialog = false
                        selectedBooking = null
                        selectedStatus = null
                        totalAmount = ""
                        cancellationReason = ""
                    },
                    enabled = selectedStatus != null
                ) {
                    Text("Saqlash", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStatusDialog = false
                        selectedBooking = null
                        selectedStatus = null
                        totalAmount = ""
                        cancellationReason = ""
                    }
                ) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundLight,
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Bronlar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = onCreateBooking) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
            }

            // Hafta navigator
            WeekNavigator(
                currentWeekStart = currentWeekStart,
                onPreviousWeek = {
                    currentWeekStart = currentWeekStart.minusWeeks(1)
                },
                onNextWeek = {
                    currentWeekStart = currentWeekStart.plusWeeks(1)
                },
                onToday = {
                    currentWeekStart = LocalDate.now()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else if (selectedDate == null) {
                // Haftalik kalendar ko'rinishi
                WeekCalendarView(
                    weekBookings = weekBookings,
                    onDayClick = { date ->
                        selectedDate = date
                    }
                )
            } else {
                // Tanlangan kun uchun bronlar ro'yxati
                DayBookingsView(
                    date = selectedDate!!,
                    bookings = weekBookings[selectedDate!!] ?: emptyList(),
                    onBack = { selectedDate = null },
                    onBookingClick = onBookingClick,
                    onEditBooking = onEditBooking,
                    onDeleteBooking = { booking ->
                        selectedBooking = booking
                        showDeleteDialog = true
                    },
                    onChangeStatus = { booking ->
                        selectedBooking = booking
                        selectedStatus = booking.status ?: BookingStatus.PENDING
                        totalAmount = booking.totalAmount?.toString() ?: ""
                        cancellationReason = booking.cancellationReason ?: ""
                        showStatusDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun WeekNavigator(
    currentWeekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit
) {
    val weekEnd = currentWeekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    LiquidGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous week",
                    tint = PrimaryGreen
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${currentWeekStart.format(formatter)} — ${weekEnd.format(formatter)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreenDark
                )

                TextButton(onClick = onToday) {
                    Text(
                        text = "Bugun",
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }

            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next week",
                    tint = PrimaryGreen
                )
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    weekBookings: Map<LocalDate, List<BookingResponse>>,
    onDayClick: (LocalDate) -> Unit
) {
    val dayNames = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba")

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(weekBookings.toList()) { (date, bookings) ->
            DayCard(
                date = date,
                dayName = dayNames[date.dayOfWeek.value - 1],
                bookingsCount = bookings.size,
                pendingCount = bookings.count { (it.status ?: BookingStatus.PENDING) == BookingStatus.PENDING },
                successfulCount = bookings.count { (it.status ?: BookingStatus.PENDING) == BookingStatus.SUCCESSFUL },
                onClick = { onDayClick(date) }
            )
        }
    }
}

@Composable
fun DayCard(
    date: LocalDate,
    dayName: String,
    bookingsCount: Int,
    pendingCount: Int,
    successfulCount: Int,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        onClick = null,
        backgroundColor = if (isToday) PrimaryGreen.copy(alpha = 0.1f) else GlassWhite
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = dayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) PrimaryGreenDark else TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = date.format(formatter),
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (bookingsCount > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bookingsCount.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pendingCount > 0) {
                            StatusIndicator(
                                count = pendingCount,
                                color = StatusPending
                            )
                        }
                        if (successfulCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            StatusIndicator(
                                count = successfulCount,
                                color = StatusSuccessful
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Bronlar yo'q",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = count.toString(),
            fontSize = 9.sp,
            color = color
        )
    }
}

@Composable
fun DayBookingsView(
    date: LocalDate,
    bookings: List<BookingResponse>,
    onBack: () -> Unit,
    onBookingClick: (BookingResponse) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit,
    onChangeStatus: (BookingResponse) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val dayNames = mapOf(
        1 to "Dushanba",
        2 to "Seshanba",
        3 to "Chorshanba",
        4 to "Payshanba",
        5 to "Juma",
        6 to "Shanba",
        7 to "Yakshanba"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Day header
        LiquidGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryGreen
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dayNames[date.dayOfWeek.value] ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreenDark
                    )
                    Text(
                        text = date.format(formatter),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Box(modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = "No bookings",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bu kun uchun bronlar yo'q",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings.sortedBy { it.bookingTime }) { booking ->
                    DayBookingCard(
                        booking = booking,
                        onClick = { onBookingClick(booking) },
                        onEdit = { onEditBooking(booking) },
                        onDelete = { onDeleteBooking(booking) },
                        onChangeStatus = { onChangeStatus(booking) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayBookingCard(
    booking: BookingResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangeStatus: () -> Unit
) {
    // Safely handle null status - default to PENDING if null
    val currentStatus = booking.status ?: BookingStatus.PENDING
    val statusColor = when (currentStatus) {
        BookingStatus.PENDING -> StatusPending
        BookingStatus.SUCCESSFUL -> StatusSuccessful
        BookingStatus.CANCELLED -> StatusCancelled
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = null
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(70.dp)
                ) {
                    Text(
                        text = booking.bookingTime.substring(0, 5),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreenDark
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = "Room",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = booking.roomName,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Guests",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${booking.guestCount}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = booking.foodDescription,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentStatus.getDisplayName(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status change button
                IconButton(
                    onClick = onChangeStatus,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChangeCircle,
                        contentDescription = "Change Status",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}