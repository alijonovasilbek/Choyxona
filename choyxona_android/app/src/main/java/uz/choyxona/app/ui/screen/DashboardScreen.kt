package uz.choyxona.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.IconChip
import uz.choyxona.app.ui.components.RangeCalendar
import uz.choyxona.app.ui.components.SectionHeader
import uz.choyxona.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class StatPeriod(val days: Long?, val label: String) {
    TODAY(1, "Bugun"),
    WEEK(7, "7 kun"),
    MONTH(30, "30 kun"),
    QUARTER(90, "90 kun"),
    CUSTOM(null, "Boshqa")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onStatsRange: (String, String) -> Unit = { _, _ -> }
) {
    val colors = LocalAppColors.current
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd.MM") }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var selectedPeriod by remember { mutableStateOf(StatPeriod.WEEK) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun applyPeriod(period: StatPeriod) {
        selectedPeriod = period
        period.days?.let { days ->
            val today = LocalDate.now()
            onStatsRange(
                today.minusDays(days - 1).format(formatter),
                today.format(formatter)
            )
        }
    }

    val rangeLabel = when {
        selectedPeriod == StatPeriod.TODAY -> "Bugun"
        selectedPeriod != StatPeriod.CUSTOM -> "Oxirgi ${selectedPeriod.label}"
        customStart != null && customEnd != null ->
            "${customStart!!.format(displayFormatter)}–${customEnd!!.format(displayFormatter)}"
        else -> "Davr tanlang"
    }

    // ===== Animated ambient background =====
    val ambient = rememberInfiniteTransition(label = "ambient")
    val drift1 by ambient.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "drift1"
    )
    val drift2 by ambient.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(11000), RepeatMode.Reverse),
        label = "drift2"
    )
    val pulse by ambient.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Soft CRM-style vertical wash from the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.10f),
                            colors.info.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Drifting glow blobs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = pulse),
                            Color.Transparent
                        ),
                        center = Offset(drift1 * 1000f + 200f, 250f),
                        radius = 650f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.info.copy(alpha = pulse * 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(drift2 * 1000f, 1500f),
                        radius = 700f
                    )
                )
        )
        // Violet accent blob (Renaissance-style tint)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = pulse * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(drift2 * 800f + 100f, 750f),
                        radius = 550f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ===== Header =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(colors.primary, colors.primaryDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.fullName?.firstOrNull() ?: 'C').uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Salom, ${currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Mehmon"} 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        if (currentUser?.filialName != null) {
                            Text(
                                text = currentUser.filialName,
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (canSwitchFilial) {
                        IconButton(onClick = onSwitchFilial) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = "Filialni almashtirish",
                                tint = colors.textSecondary
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sozlamalar",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Period filter =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPeriod.values().forEach { period ->
                        PeriodChip(
                            label = if (period == StatPeriod.CUSTOM) {
                                if (selectedPeriod == StatPeriod.CUSTOM &&
                                    customStart != null && customEnd != null
                                ) rangeLabel else period.label
                            } else period.label,
                            selected = selectedPeriod == period,
                            withIcon = period == StatPeriod.CUSTOM,
                            onClick = {
                                if (period == StatPeriod.CUSTOM) {
                                    showCalendar = true
                                } else {
                                    applyPeriod(period)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Hero stat =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 100)) +
                        slideInVertically(tween(500, delayMillis = 100)) { it / 4 }
            ) {
                HeroStatCard(
                    value = stats?.totalBookings ?: 0,
                    rangeLabel = rangeLabel
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Sections =====
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 200)) +
                        slideInVertically(tween(600, delayMillis = 200)) { it / 4 }
            ) {
                Column {
                    SectionHeader(title = "Bo'limlar")
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            title = "Bronlar",
                            subtitle = "Haftalik jadval",
                            icon = Icons.Default.EventNote,
                            tint = colors.primary,
                            onClick = onNavigateToBookings,
                            modifier = Modifier.weight(1f)
                        )
                        ActionCard(
                            title = "Xonalar",
                            subtitle = "Xona va so'rilar",
                            icon = Icons.Default.MeetingRoom,
                            tint = colors.info,
                            onClick = onNavigateToRooms,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ===== Custom range calendar sheet =====
        if (showCalendar) {
            ModalBottomSheet(
                onDismissRequest = { showCalendar = false },
                sheetState = sheetState,
                containerColor = colors.surfaceElevated,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.border)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "Davr tanlang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    RangeCalendar(
                        startDate = customStart,
                        endDate = customEnd,
                        onRangeChange = { start, end ->
                            customStart = start
                            customEnd = end
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceAlt)
                                .clickable {
                                    customStart = null
                                    customEnd = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tozalash",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        }

                        Box(modifier = Modifier.weight(2f)) {
                            GlassButton(
                                text = "Qo'llash",
                                enabled = customStart != null,
                                onClick = {
                                    val start = customStart ?: return@GlassButton
                                    val end = customEnd ?: start
                                    customEnd = end
                                    selectedPeriod = StatPeriod.CUSTOM
                                    onStatsRange(
                                        start.format(formatter),
                                        end.format(formatter)
                                    )
                                    scope.launch {
                                        sheetState.hide()
                                    }.invokeOnCompletion {
                                        showCalendar = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    withIcon: Boolean = false
) {
    val colors = LocalAppColors.current
    val bg by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surface,
        animationSpec = tween(250),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.textOnPrimary else colors.textSecondary,
        animationSpec = tween(250),
        label = "chipFg"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (withIcon) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun HeroStatCard(
    value: Int,
    rangeLabel: String
) {
    val colors = LocalAppColors.current
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "heroValue"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(colors.primary, colors.primaryDark)
                )
            )
    ) {
        // Decorative translucent circles
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = 40.dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Jami bronlar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = animatedValue.toString(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rangeLabel,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = 16.dp
    ) {
        IconChip(icon = icon, tint = tint, size = 44)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = colors.textTertiary
        )
    }
}

data class DashboardStats(
    val totalBookings: Int = 0,
    val pendingCount: Int = 0,
    val successfulCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalRevenue: Double = 0.0
)
