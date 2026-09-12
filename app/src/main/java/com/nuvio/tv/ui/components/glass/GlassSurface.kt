package com.nuvio.tv.ui.components.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.nuvio.tv.ui.theme.NuvioGlass
import com.nuvio.tv.ui.theme.NuvioStrokes
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * The backdrop every glass surface samples. Provided by the Glass home layout around the hero
 * image; null anywhere the layout is not active, which makes surfaces fall back to a flat tint.
 */
val LocalGlassHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * True when glass surfaces should sample a live blur. The Glass layout sets this to false during
 * playback, where controls sit over moving video and a blur would cost a render pass per frame for
 * no legibility gain.
 */
val LocalGlassBlurEnabled = staticCompositionLocalOf { true }

/**
 * Lets scrolling content ask for the top chrome when the user presses Up with nowhere left to go.
 *
 * This has to come from the content, not from a key handler on the scaffold. Vertical row movement
 * in the home rows is performed by Compose's default focus traversal, which runs *after* both key
 * phases — so a handler at the root consumes Up before focus ever gets the chance to move, and the
 * bar would appear from anywhere on the page. The row list already knows when it is on its first
 * row; this is the hook it calls at that moment.
 *
 * Returns true when the chrome took the key, so the caller consumes it.
 */
val LocalGlassChromeReveal = staticCompositionLocalOf<() -> Boolean> { { false } }

/** Neutral, very slightly cool base so the tint reads as glass rather than as a grey card. */
private val GlassBase = Color(0xFF16161A)

/**
 * Frosted surface treatment: blur what is behind, tint it, then define the edge.
 *
 * The edge is what actually sells glass. A flat border looks like a stroke on a rectangle, so the
 * hairline runs from bright at the top to nearly invisible at the bottom, the way a real bevel
 * catches light from above, and the tint carries a matching vertical falloff.
 *
 * Falls back to an opaque tint with no blur when [LocalGlassHazeState] is absent, when blur is
 * switched off, or on pre-Android-12 hardware that has no RenderEffect. The surface still reads
 * correctly in all three cases; it just stops being see-through.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    focused: Boolean = false,
    strong: Boolean = false,
    hazeState: HazeState? = LocalGlassHazeState.current,
    blurEnabled: Boolean = LocalGlassBlurEnabled.current
): Modifier {
    val tokens = NuvioGlass.tokens
    val liveBlur = blurEnabled && hazeState != null && NuvioGlass.isLiveBlurSupported

    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) tokens.borderFocusedAlpha else tokens.borderTopAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glassBorderAlpha"
    )

    val tintBrush = remember(liveBlur) {
        val alpha = if (liveBlur) tokens.tintAlpha else tokens.tintAlphaFallback
        Brush.verticalGradient(
            listOf(
                GlassBase.copy(alpha = (alpha + tokens.highlightAlpha).coerceAtMost(1f)),
                GlassBase.copy(alpha = alpha)
            )
        )
    }

    val borderBrush = remember(borderAlpha, focused) {
        val bottom = if (focused) borderAlpha * 0.4f else NuvioGlass.tokens.borderBottomAlpha
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = bottom)
            )
        )
    }

    return this
        .clip(shape)
        .then(
            if (liveBlur) {
                Modifier.hazeEffect(state = hazeState!!) {
                    blurRadius = if (strong) tokens.blurRadiusStrong else tokens.blurRadius
                    noiseFactor = tokens.noiseFactor
                    inputScale = HazeInputScale.Fixed(tokens.inputScale)
                }
            } else {
                Modifier
            }
        )
        .background(brush = tintBrush, shape = shape)
        .border(width = NuvioStrokes.tokens.hairline, brush = borderBrush, shape = shape)
}

/** Convenience container for content that should sit on a glass surface. */
@Composable
fun GlassPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    strong: Boolean = false,
    blurEnabled: Boolean = LocalGlassBlurEnabled.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassSurface(
            shape = shape,
            focused = focused,
            strong = strong,
            blurEnabled = blurEnabled
        ),
        content = content
    )
}
