package uz.choyxona.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.YearMonth

private val UZBEK_MONTHS = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
    "Iyul", "Avgust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr"
)
private val UZBEK_WEEKDAYS = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")

/**
 * Fully custom range calendar: swipe-free month navigation with slide
 * animations, range selection with connected band highlighting.
 */
@Composable
fun RangeCalendar(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(startDate ?: today)) }
    // 1 = forward, -1 = backward; drives slide direction
    var navDirection by remember { mutableIntStateOf(1) }

    Column(modifier = modifier.fillMaxWidth()) {
        // ===== Month header =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    navDirection = -1
                    visibleMonth = visibleMonth.minusMonths(1)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceAlt)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Oldingi oy",
                    tint = colors.textPrimary
                )
            }

            AnimatedContent(
                targetState = visibleMonth,
                transitionSpec = {
                    (slideInHorizontally(tween(250)) { navDirection * it / 2 } + fadeIn(tween(250)))
                        .togetherWith(
                            slideOutHorizontally(tween(250)) { -navDirection * it / 2 } +
                                fadeOut(tween(200))
                        )
                },
                label = "monthTitle"
            ) { month ->
                Text(
                    text = "${UZBEK_MONTHS[month.monthValue - 1]} ${month.year}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }

            IconButton(
                onClick = {
                    navDirection = 1
                    visibleMonth = visibleMonth.plusMonths(1)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceAlt)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Keyingi oy",
                    tint = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== Weekday labels =====
        Row(modifier = Modifier.fillMaxWidth()) {
            UZBEK_WEEKDAYS.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ===== Day grid (animated per month) =====
        AnimatedContent(
            targetState = visibleMonth,
            transitionSpec = {
                (slideInHorizontally(tween(250)) { navDirection * it / 3 } + fadeIn(tween(250)))
                    .togetherWith(
                        slideOutHorizontally(tween(250)) { -navDirection * it / 3 } +
                            fadeOut(tween(200))
                    )
            },
            label = "monthGrid"
        ) { month ->
            val firstDay = month.atDay(1)
            // Monday-first offset
            val leadingEmpty = (firstDay.dayOfWeek.value + 6) % 7
            val daysInMonth = month.lengthOfMonth()
            val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7

            Column {
                for (week in 0 until totalCells / 7) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (dayOfWeek in 0 until 7) {
                            val cellIndex = week * 7 + dayOfWeek
                            val dayNumber = cellIndex - leadingEmpty + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = month.atDay(dayNumber)
                                DayCell(
                                    date = date,
                                    today = today,
                                    startDate = startDate,
                                    endDate = endDate,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when {
                                            startDate == null || endDate != null -> {
                                                onRangeChange(date, null)
                                            }
                                            date.isBefore(startDate) -> {
                                                onRangeChange(date, null)
                                            }
                                            else -> {
                                                onRangeChange(startDate, date)
                                            }
                                        }
                                    }
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val isStart = date == startDate
    val isEnd = date == endDate
    val isEndpoint = isStart || isEnd
    val inRange = startDate != null && endDate != null &&
        !date.isBefore(startDate) && !date.isAfter(endDate)
    val isToday = date == today
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Range band behind endpoints/in-between days
        if (inRange && startDate != endDate) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .fillMaxWidth()
                    .padding(
                        start = if (isStart) 12.dp else 0.dp,
                        end = if (isEnd) 12.dp else 0.dp
                    )
                    .background(colors.primaryContainer)
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isEndpoint) colors.primary else androidx.compose.ui.graphics.Color.Transparent
                )
                .then(
                    if (isToday && !isEndpoint) {
                        Modifier.border(1.5.dp, colors.primary, CircleShape)
                    } else Modifier
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isEndpoint || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isEndpoint -> colors.textOnPrimary
                    inRange -> colors.primary
                    isToday -> colors.primary
                    else -> colors.textPrimary
                }
            )
        }
    }
}
