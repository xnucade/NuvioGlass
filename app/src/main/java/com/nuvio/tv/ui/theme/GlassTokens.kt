package com.nuvio.tv.ui.theme

import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens for the Glass home layout's frosted chrome.
 *
 * Kept separate from [NuvioEffectTokens] because glass is a distinct surface treatment with its
 * own tint/border/highlight relationship, not just a blur radius. Values are tuned for the Onn 4K:
 * a 1080p output where the blur is recomputed only when focus moves the hero backdrop.
 */
@Immutable
data class NuvioGlassTokens(
    /** Height of the nav and clock pills. Large enough for a 10-foot UI to read at a glance. */
    val pillHeight: Dp,
    val pillHorizontalPadding: Dp,
    val pillItemGap: Dp,
    val pillItemHorizontalPadding: Dp,
    val badgeHeight: Dp,
    val badgeHorizontalPadding: Dp,
    /** Blur applied to chrome that sits over the hero. */
    val blurRadius: Dp,
    /** Heavier blur for surfaces that must stay legible over busy artwork. */
    val blurRadiusStrong: Dp,
    /**
     * Haze renders the blur at this fraction of the source size. 0.66 matches the sidebar panel
     * upstream and is the main reason the effect stays cheap on a 2 GB box.
     */
    val inputScale: Float,
    val noiseFactor: Float,
    /** Tint alpha when a live blur sits behind the surface. */
    val tintAlpha: Float,
    /** Tint alpha on devices with no live blur, where the tint alone has to do the work. */
    val tintAlphaFallback: Float,
    /** Top edge of the hairline border, where a real glass edge catches the most light. */
    val borderTopAlpha: Float,
    val borderBottomAlpha: Float,
    val borderFocusedAlpha: Float,
    /** Inner highlight running along the top of the surface. */
    val highlightAlpha: Float,
    /** Scrim under the chrome so white text survives a bright backdrop. */
    val scrimAlpha: Float,
    /** Scale applied to a focused pill item. */
    val focusScale: Float,
    /** Scale applied to a focused poster card. */
    val cardFocusScale: Float
)

object NuvioGlass {
    val tokens = NuvioGlassTokens(
        pillHeight = 52.dp,
        pillHorizontalPadding = 6.dp,
        pillItemGap = 2.dp,
        pillItemHorizontalPadding = 18.dp,
        badgeHeight = 24.dp,
        badgeHorizontalPadding = 10.dp,
        blurRadius = 24.dp,
        blurRadiusStrong = 36.dp,
        inputScale = 0.66f,
        noiseFactor = 0.04f,
        tintAlpha = 0.28f,
        tintAlphaFallback = 0.82f,
        borderTopAlpha = 0.22f,
        borderBottomAlpha = 0.07f,
        borderFocusedAlpha = 0.55f,
        highlightAlpha = 0.10f,
        scrimAlpha = 0.30f,
        focusScale = 1.06f,
        cardFocusScale = 1.08f
    )

    /**
     * Live blur needs [android.graphics.RenderEffect], added in Android 12 (API 31).
     *
     * Both Onn 4K boxes clear this: the base box ships Android 12 and the Pro ships Android 14.
     * A Shield TV (Android 11) and most Fire TV sticks (Fire OS 7/8) do not, and fall back to the
     * opaque tint instead of dropping frames trying to blur moving artwork.
     */
    val isLiveBlurSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
