package com.nuvio.tv.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioGlass
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Clock pill for the top-right of the Glass home screen.
 *
 * Ticks on the minute rather than the second. A per-second tick would redraw the pill 60 times a
 * minute over the hero, and on a box this size that is wasted work for a clock nobody reads to the
 * second.
 */
@Composable
fun GlassClockPill(
    modifier: Modifier = Modifier,
    use24Hour: Boolean = false,
    showDate: Boolean = true
) {
    val tokens = NuvioGlass.tokens
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        androidx.core.os.ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }

    val timeFormatter = remember(use24Hour, locale) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
    }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM", locale) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Sleep to the top of the next minute so the clock flips when it actually changes.
            val millisToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(millisToNextMinute)
        }
    }

    GlassPanel(
        shape = RoundedCornerShape(NuvioRadii.tokens.full),
        modifier = modifier.height(tokens.pillHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = NuvioTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
        ) {
            Text(
                text = now.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            if (showDate) {
                Text(
                    text = now.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f)
                )
            }
        }
    }
}
