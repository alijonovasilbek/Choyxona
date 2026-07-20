package uz.choyxona.app.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.BuildConfig
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.IconChip
import uz.choyxona.app.ui.components.SectionHeader
import uz.choyxona.app.ui.components.StatusChip
import uz.choyxona.app.ui.theme.LocalAppColors
import uz.choyxona.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    currentUser: UserResponse?,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onSwitchFilial: () -> Unit,
    canSwitchFilial: Boolean,
    onLogout: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Orqaga",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sozlamalar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== Profile card =====
            GlassCard(contentPadding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.fullName?.firstOrNull() ?: 'C').uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "—",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "@${currentUser?.username ?: "—"}",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        if (currentUser?.filialName != null) {
                            Text(
                                text = currentUser.filialName,
                                fontSize = 13.sp,
                                color = colors.textTertiary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        currentUser?.roles?.forEach { role ->
                            StatusChip(
                                text = role,
                                color = when (role) {
                                    "superadmin" -> colors.statusCancelled
                                    "admin" -> colors.info
                                    else -> colors.primary
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Appearance =====
            SectionHeader(title = "Ko'rinish")
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeOptionCard(
                    title = "Tizim",
                    icon = Icons.Default.BrightnessAuto,
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionCard(
                    title = "Yorug'",
                    icon = Icons.Default.LightMode,
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionCard(
                    title = "Tungi",
                    icon = Icons.Default.DarkMode,
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== Account actions =====
            SectionHeader(title = "Hisob")
            Spacer(modifier = Modifier.height(10.dp))

            if (canSwitchFilial) {
                GlassCard(
                    contentPadding = 16.dp,
                    onClick = onSwitchFilial
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(icon = Icons.Default.Store, tint = colors.info)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Filialni almashtirish",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Boshqa filial ma'lumotlarini ko'rish",
                                fontSize = 12.sp,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            GlassCard(
                contentPadding = 16.dp,
                onClick = onLogout
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(icon = Icons.Default.Logout, tint = colors.error)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Chiqish",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Choyxona v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.border,
        animationSpec = tween(250),
        label = "themeCardBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surface,
        animationSpec = tween(250),
        label = "themeCardBg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) colors.primary else colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.primary else colors.textSecondary
        )
    }
}
