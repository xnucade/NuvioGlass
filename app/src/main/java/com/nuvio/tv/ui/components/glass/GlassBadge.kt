package com.nuvio.tv.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioGlass
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Small frosted badge for hero metadata: quality, rating, runtime, audio format.
 *
 * Badges are the one glass element that appears many times on screen at once, so each one is a
 * blur pass. Keep a hero to a handful. If a row ever needs a badge per card, use [flat] to drop
 * the blur and keep only the tint.
 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    flat: Boolean = false
) {
    val tokens = NuvioGlass.tokens
    val shape = RoundedCornerShape(NuvioRadii.tokens.sm)

    GlassPanel(
        shape = shape,
        modifier = modifier.height(tokens.badgeHeight),
        // A badge is small enough that a heavy blur would sample almost nothing useful.
        strong = false,
        blurEnabled = !flat && LocalGlassBlurEnabled.current
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = tokens.badgeHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(NuvioTheme.sizes.icons.xs)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}
