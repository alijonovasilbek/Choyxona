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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.model.RoomResponse
import uz.choyxona.app.data.model.FilialInfo
import uz.choyxona.app.ui.components.FilialSwitcher
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*
import uz.choyxona.app.ui.util.roomNameComparator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun WeeklyBookingsScreen(
    bookings: List<BookingResponse>,
    rooms: List<RoomResponse>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBookingClick: (BookingResponse) -> Unit,
    onNavigateBack: () -> Unit,
    onCreateBooking: () -> Unit,
    onCreateBookingForRoomDate: (roomId: Int, date: LocalDate) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit,
    availableFilials: List<FilialInfo> = emptyList(),
    activeFilialId: Int? = null,
    canSwitchFilial: Boolean = false,
    onFilialSelected: (Int) -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentWeekStart by remember { mutableStateOf(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedBooking by remember { mutableStateOf<BookingResponse?>(null) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Bronlar",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    FilialSwitcher(
                        filials = availableFilials,
                        activeFilialId = activeFilialId,
                        onFilialSelected = onFilialSelected,
                        enabled = canSwitchFilial,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

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
                    selectedDate = null // Reset selected date when changing weeks
                },
                onNextWeek = {
                    currentWeekStart = currentWeekStart.plusWeeks(1)
                    selectedDate = null // Reset selected date when changing weeks
                },
                onToday = {
                    currentWeekStart = LocalDate.now()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    selectedDate = null // Reset selected date
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
                    rooms = rooms,
                    bookings = weekBookings[selectedDate!!] ?: emptyList(),
                    onBack = { selectedDate = null },
                    onCreateBookingForRoomDate = onCreateBookingForRoomDate,
                    onEditBooking = onEditBooking,
                    onDeleteBooking = { booking ->
                        selectedBooking = booking
                        showDeleteDialog = true
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
                    fontSize = 14.sp,
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
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(weekBookings.toList()) { (date, bookings) ->
            DayCard(
                date = date,
                dayName = dayNames[date.dayOfWeek.value - 1],
                bookingsCount = bookings.size,
                pendingCount = bookings.count { it.safeStatus() == BookingStatus.KUTILMOQDA },
                successfulCount = bookings.count { it.safeStatus() == BookingStatus.MUVAFFAQIYATLI },
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
            .height(140.dp)  // Kichikroq qilindi (eski 160dp edi)
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
                fontSize = 13.sp,  // Kichikroq qilindi (eski 14sp edi)
                fontWeight = FontWeight.Bold,
                color = if (isToday) PrimaryGreenDark else TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = date.format(formatter),
                fontSize = 11.sp,  // Kichikroq qilindi (eski 12sp edi)
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (bookingsCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(PrimaryGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$bookingsCount ta ${if (bookingsCount > 1) "bronlar" else "bron"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreenDark
                    )
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
    rooms: List<RoomResponse>,
    bookings: List<BookingResponse>,
    onBack: () -> Unit,
    onCreateBookingForRoomDate: (roomId: Int, date: LocalDate) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit
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
    val bookingsByRoomId = remember(bookings) { bookings.groupBy { it.roomId } }
    val knownRoomIds = remember(rooms) { rooms.map { it.id }.toSet() }
    val dayRooms = remember(rooms, bookingsByRoomId, knownRoomIds, bookings) {
        val roomsFromCatalog = rooms
            .sortedWith(roomNameComparator { it.name })
            .map { room ->
                DayRoomBookings(
                    roomId = room.id,
                    roomName = room.name,
                    bookings = bookingsByRoomId[room.id]
                        .orEmpty()
                        .sortedBy { it.safeBookingTimeSortKey() }
                )
            }

        val missingRooms = bookings
            .filter { it.roomId !in knownRoomIds }
            .groupBy { it.roomId to it.roomName }
            .values
            .map { roomBookings ->
                val firstBooking = roomBookings.first()
                DayRoomBookings(
                    roomId = firstBooking.roomId,
                    roomName = firstBooking.roomName,
                    bookings = roomBookings.sortedBy { it.safeBookingTimeSortKey() }
                )
            }
            .sortedWith(roomNameComparator { it.roomName })

        roomsFromCatalog + missingRooms
    }

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

        if (dayRooms.isEmpty()) {
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
                items(
                    items = dayRooms,
                    key = { it.roomId }
                ) { roomData ->
                    DayRoomBookingsCard(
                        roomId = roomData.roomId,
                        roomName = roomData.roomName,
                        bookings = roomData.bookings,
                        onCreateBookingForRoom = { roomId ->
                            onCreateBookingForRoomDate(roomId, date)
                        },
                        onEditBooking = onEditBooking,
                        onDeleteBooking = onDeleteBooking
                    )
                }
            }
        }
    }
}

private data class DayRoomBookings(
    val roomId: Int,
    val roomName: String,
    val bookings: List<BookingResponse>
)

@Composable
private fun DayRoomBookingsCard(
    roomId: Int,
    roomName: String,
    bookings: List<BookingResponse>,
    onCreateBookingForRoom: (roomId: Int) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = roomName.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bookings.isEmpty()) {
                    IconButton(
                        onClick = { onCreateBookingForRoom(roomId) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Create booking",
                            tint = PrimaryGreen
                        )
                    }
                }
            }
        }

        if (bookings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))
            bookings.forEachIndexed { index, booking ->
                DayBookingCard(
                    booking = booking,
                    onEdit = { onEditBooking(booking) },
                    onDelete = { onDeleteBooking(booking) }
                )
                if (index < bookings.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun DayBookingCard(
    booking: BookingResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 8.dp,
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = booking.safeDescription(),
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun BookingResponse.safeStatus(): BookingStatus {
    return (this.status as? BookingStatus) ?: BookingStatus.KUTILMOQDA
}

private fun BookingResponse.safeBookingTimeSortKey(): String {
    return (this.bookingTime as? String).orEmpty()
}

private fun BookingResponse.safeDescription(): String {
    val desc = this.description?.trim().orEmpty()
    if (desc.isNotEmpty()) return desc
    val food = (this.foodDescription as? String).orEmpty().trim()
    return if (food.isNotEmpty()) food else "Tavsif yo'q"
}
