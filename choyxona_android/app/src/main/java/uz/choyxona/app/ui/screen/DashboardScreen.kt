package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.BackgroundLight
import uz.choyxona.app.ui.theme.PrimaryGreen
import uz.choyxona.app.ui.theme.PrimaryGreenDark
import uz.choyxona.app.ui.theme.StatusCancelled
import uz.choyxona.app.ui.theme.StatusPending
import uz.choyxona.app.ui.theme.StatusSuccessful
import uz.choyxona.app.ui.theme.TextPrimary
import uz.choyxona.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    currentUser: UserResponse?,
    stats: DashboardStats?,
    onNavigateToBookings: () -> Unit,
    onNavigateToRooms: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onSwitchFilial: () -> Unit,
    canSwitchFilial: Boolean = false,
    onLogout: () -> Unit
) {
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
                Column {
                    Text(
                        text = "Salom, ${currentUser?.fullName?.split(" ")?.firstOrNull() ?: "User"}!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canSwitchFilial) {
                        IconButton(
                            onClick = onSwitchFilial,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            PrimaryGreen,
                                            PrimaryGreenDark
                                        )
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = "Filialni tanlash",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        PrimaryGreen,
                                        PrimaryGreenDark
                                    )
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Check if user is superadmin
            val isSuperAdmin = currentUser?.roles?.contains("superadmin") == true

            if (isSuperAdmin) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton("Bronlar", Icons.Default.Book, onNavigateToBookings)
                    QuickActionButton("Xonalar", Icons.Default.MeetingRoom, onNavigateToRooms)
                    QuickActionButton("Userlar", Icons.Default.People, onNavigateToUsers)
                }

            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "Bronlar",
                        icon = Icons.Default.Book,
                        onClick = onNavigateToBookings
                    )

                    QuickActionButton(
                        title = "Xonalar",
                        icon = Icons.Default.MeetingRoom,
                        onClick = onNavigateToRooms
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
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
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryGreen,
                                PrimaryGreenDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

data class DashboardStats(
    val totalBookings: Int = 0,
    val pendingCount: Int = 0,
    val successfulCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalRevenue: Double = 0.0
)
