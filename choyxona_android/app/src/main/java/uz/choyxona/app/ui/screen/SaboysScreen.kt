package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.SaboyResponse
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.LocalAppColors

@Composable
fun SaboysScreen(
    saboys: List<SaboyResponse>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onCreateSaboy: () -> Unit,
    onEditSaboy: (SaboyResponse) -> Unit,
    onDeleteSaboy: (SaboyResponse) -> Unit
) {
    val colors = LocalAppColors.current
    var pendingDelete by remember { mutableStateOf<SaboyResponse?>(null) }

    // Sanaga qarab guruhlanadi, eng yaqin sana yuqorida.
    val grouped = remember(saboys) {
        saboys.sortedWith(compareBy({ it.saboyDate }, { it.saboyTime }))
            .groupBy { it.saboyDate }
    }

    pendingDelete?.let { saboy ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Saboyni o'chirish") },
            text = { Text("${formatSaboyDate(saboy.saboyDate)} kungi saboyni o'chirmoqchimisiz?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSaboy(saboy)
                    pendingDelete = null
                }) {
                    Text("O'chirish", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                        contentDescription = "Back",
                        tint = colors.primary
                    )
                }

                Text(
                    text = "Saboylar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                IconButton(onClick = onCreateSaboy) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Saboy qo'shish",
                        tint = colors.primary
                    )
                }
            }

            when {
                isLoading && saboys.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                saboys.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Saboylar yo'q",
                                fontSize = 16.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        grouped.forEach { (date, daySaboys) ->
                            item(key = date) {
                                LiquidGlassCard {
                                    Text(
                                        text = formatSaboyDate(date),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    daySaboys.forEachIndexed { index, saboy ->
                                        SaboyRow(
                                            saboy = saboy,
                                            onEdit = { onEditSaboy(saboy) },
                                            onDelete = { pendingDelete = saboy }
                                        )
                                        if (index < daySaboys.lastIndex) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaboyRow(
    saboy: SaboyResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp,
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatSaboyTime(saboy.saboyTime),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = saboy.description,
                fontSize = 14.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Tahrirlash",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "O'chirish",
                    tint = colors.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (!saboy.createdByName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Yaratdi: ${saboy.createdByName}",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

/** "2026-07-28" -> "28.07.2026"; noma'lum format o'zgarishsiz qaytadi. */
fun formatSaboyDate(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else isoDate
}

/** "14:30:00" -> "14:30". */
fun formatSaboyTime(isoTime: String): String {
    val parts = isoTime.split(":")
    return if (parts.size >= 2) "${parts[0]}:${parts[1]}" else isoTime
}
