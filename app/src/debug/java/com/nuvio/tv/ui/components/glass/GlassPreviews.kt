package com.nuvio.tv.ui.components.glass

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Previews for the glass chrome.
 *
 * **What these can and cannot show.** Compose Preview renders through Layoutlib, which does not
 * implement `RenderEffect`, so the Haze blur is absent here no matter what the code does. What you
 * are judging in the IDE is tint, edge, radius, spacing, type and focus states. To see the actual
 * frost, run on the TV emulator or the Onn.
 *
 * The backdrop below stands in for a hero image: a warm-to-cool diagonal with a bright band placed
 * where the pills sit, because a glass edge only proves itself against a light-to-dark transition.
 */
@Composable
private fun PreviewBackdrop(content: @Composable BoxScope.() -> Unit) {
    NuvioTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB4531E),
                            Color(0xFF7A2E52),
                            Color(0xFF16203A),
                            Color(0xFF07070B)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1600f, 900f)
                    )
                )
        ) {
            // A bright band under the chrome, so the hairline edge has something to read against.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0x66FFD9A0), Color.Transparent)
                        )
                    )
            )
            content()
        }
    }
}

private val previewNavItems = listOf(
    GlassNavItem("home", "Home", Icons.Default.Home),
    GlassNavItem("search", "Search", Icons.Default.Search),
    GlassNavItem("discover", "Discover", Icons.Default.Star),
    GlassNavItem("library", "Library", Icons.AutoMirrored.Filled.List),
    GlassNavItem("settings", "Settings", Icons.Default.Settings)
)

@Preview(
    name = "Glass chrome — nav + clock",
    widthDp = 960,
    heightDp = 540,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION
)
@Composable
fun GlassChromePreview() {
    PreviewBackdrop {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = NuvioTheme.spacing.screen.overscanHorizontal,
                    vertical = NuvioTheme.spacing.screen.vertical
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassNavPill(
                items = previewNavItems,
                selectedRoute = "home",
                onNavigate = {}
            )
            GlassClockPill()
        }
    }
}

@Preview(
    name = "Glass badges",
    widthDp = 960,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION
)
@Composable
fun GlassBadgesPreview() {
    PreviewBackdrop {
        Row(
            modifier = Modifier.padding(NuvioTheme.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBadge(text = "4K HDR")
            GlassBadge(text = "8.4", icon = Icons.Default.Star)
            GlassBadge(text = "2h 18m")
            GlassBadge(text = "Atmos")
            GlassBadge(text = "flat", flat = true)
        }
    }
}

/**
 * The surface on its own, at panel scale, so the tint gradient and the edge falloff can be read
 * without pill geometry in the way. Second panel shows the focused edge.
 */
@Preview(
    name = "Glass surface — rest vs focused",
    widthDp = 960,
    heightDp = 360,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION
)
@Composable
fun GlassSurfacePreview() {
    PreviewBackdrop {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(NuvioTheme.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xl)
        ) {
            listOf("At rest" to false, "Focused" to true).forEach { (label, focused) ->
                GlassPanel(
                    shape = RoundedCornerShape(NuvioRadii.tokens.panel),
                    focused = focused,
                    modifier = Modifier
                        .size(width = 380.dp, height = 220.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(NuvioTheme.spacing.xl),
                        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Blur is absent in Preview. Edge and tint are accurate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
