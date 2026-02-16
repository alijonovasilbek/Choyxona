package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.model.getDisplayName
import uz.choyxona.app.data.model.getColorCode
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.theme.BackgroundLight
import uz.choyxona.app.ui.theme.PrimaryGreen
import uz.choyxona.app.ui.theme.StatusCancelled
import uz.choyxona.app.ui.theme.StatusPending
import uz.choyxona.app.ui.theme.StatusSuccessful
import uz.choyxona.app.ui.theme.TextPrimary
import uz.choyxona.app.ui.theme.TextSecondary

@Composable
fun BookingsScreen(
    bookings: List<BookingResponse>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBookingClick: (BookingResponse) -> Unit,
    onNavigateBack: () -> Unit,
    onCreateBooking: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf<BookingStatus?>(null) }

    val filteredBookings = if (selectedStatus != null) {
        bookings.filter { it.status == selectedStatus }
    } else {
        bookings
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

            // Status Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("Barchasi") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedStatus == BookingStatus.KUTILMOQDA,
                    onClick = { selectedStatus = BookingStatus.KUTILMOQDA },
                    label = { Text("Kutilmoqda") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedStatus == BookingStatus.MUVAFFAQIYATLI,
                    onClick = { selectedStatus = BookingStatus.MUVAFFAQIYATLI },
                    label = { Text("Muvaffaqiyatli") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedStatus == BookingStatus.BEKOR_QILINDI,
                    onClick = { selectedStatus = BookingStatus.BEKOR_QILINDI },
                    label = { Text("Bekor") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryGreen
                    )
                }
            } else if (filteredBookings.isEmpty()) {
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
                            text = "Bronlar yo'q",
                            fontSize = 18.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings) { booking ->
                        BookingCard(
                            booking = booking,
                            onClick = { onBookingClick(booking) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingResponse,
    onClick: () -> Unit
) {
    val statusColor = when (booking.status) {
        BookingStatus.KUTILMOQDA -> StatusPending
        BookingStatus.MUVAFFAQIYATLI -> StatusSuccessful
        BookingStatus.BEKOR_QILINDI -> StatusCancelled
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = booking.roomName.uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (booking.customerName.isNotBlank()) {
                    Text(
                        text = booking.customerName,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = booking.bookingDate,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Time",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = booking.bookingTime,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Guests",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.guestCount} kishi",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = booking.status.getDisplayName(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val descriptionText = booking.description
            ?.takeIf { it.isNotBlank() }
            ?: booking.foodDescription.takeIf { it.isNotBlank() }
            ?: "Tavsif yo'q"

        Text(
            text = descriptionText,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}
