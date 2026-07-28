package uz.choyxona.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.FilialInfo
import uz.choyxona.app.ui.theme.LocalAppColors

/**
 * Compact filial (branch) picker meant to sit in a screen header.
 *
 * Renders nothing when the user cannot switch or when there is only one filial
 * to pick from, so oshpaz headers stay unchanged.
 */
@Composable
fun FilialSwitcher(
    filials: List<FilialInfo>,
    activeFilialId: Int?,
    onFilialSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled || filials.size < 2) return

    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    val activeName = filials.firstOrNull { it.id == activeFilialId }?.name
        ?: "Filial tanlang"

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(colors.primary.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = colors.primary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(999.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = activeName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Filialni almashtirish",
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filials.forEach { filial ->
                val isActive = filial.id == activeFilialId
                DropdownMenuItem(
                    text = {
                        Text(
                            text = filial.name,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) colors.primary else colors.textPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = if (isActive) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tanlangan",
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(18.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        if (!isActive) onFilialSelected(filial.id)
                    }
                )
            }
        }
    }
}
