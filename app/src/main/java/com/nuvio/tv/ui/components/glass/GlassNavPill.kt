package com.nuvio.tv.ui.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.screens.settings.rememberRawSvgPainter
import com.nuvio.tv.ui.theme.NuvioGlass
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioTheme

data class GlassNavItem(
    val route: String,
    val label: String,
    /** Vector icon, or null when the item ships a drawable via [iconRes]. */
    val icon: ImageVector? = null,
    val iconRes: Int? = null
)

/** Settles quickly with a trace of overshoot — enough to feel alive, not enough to wobble. */
private val IndicatorSpring = spring<Dp>(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * The top navigation pill.
 *
 * One glass surface holds every destination, so the whole bar costs a single blur pass rather than
 * one per item. Inside it, a single indicator chip **slides** between destinations instead of each
 * item fading its own background in and out. That is the difference between a bar that feels like
 * one object and a row of buttons that happen to sit together, and it costs one animated offset
 * rather than one animation per item.
 *
 * Labels belong to the active destination only, so the bar reads as icons at rest and opens up
 * where your attention already is.
 */
@Composable
fun GlassNavPill(
    items: List<GlassNavItem>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val tokens = NuvioGlass.tokens
    val pillShape = RoundedCornerShape(NuvioRadii.tokens.full)
    val itemShape = RoundedCornerShape(NuvioRadii.tokens.full)
    val density = LocalDensity.current

    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.takeIf { it >= 0 } ?: 0
    // The indicator tracks focus while the bar is being driven, and falls back to the current
    // destination once focus leaves, so it never points at nothing.
    val activeIndex = focusedIndex ?: selectedIndex

    // Item geometry, measured rather than assumed, because label width varies per language.
    val bounds: SnapshotStateMap<Int, Pair<Dp, Dp>> = remember { mutableStateMapOf() }
    var rowOriginX by remember { mutableStateOf(0f) }
    val active = bounds[activeIndex]

    val indicatorX by animateDpAsState(
        targetValue = active?.first ?: 0.dp,
        animationSpec = IndicatorSpring,
        label = "navIndicatorX"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = active?.second ?: 0.dp,
        animationSpec = IndicatorSpring,
        label = "navIndicatorWidth"
    )
    // Focus makes the chip opaque white; unfocused it is a faint marker of where you are.
    val indicatorColor by animateColorAsState(
        targetValue = if (focusedIndex != null) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.13f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navIndicatorColor"
    )

    GlassPanel(
        shape = pillShape,
        modifier = modifier
            .height(tokens.pillHeight)
            .onFocusChanged { state ->
                onFocusChanged(state.hasFocus)
                if (!state.hasFocus) focusedIndex = null
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = tokens.pillHorizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            if (active != null) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX)
                        .width(indicatorWidth)
                        .height(tokens.pillHeight - 12.dp)
                        .clip(itemShape)
                        .background(indicatorColor)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .onGloballyPositioned { rowOriginX = it.positionInRoot().x },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.pillItemGap)
            ) {
                items.forEachIndexed { index, item ->
                    GlassNavPillItem(
                        item = item,
                        isSelected = index == selectedIndex,
                        isFocused = focusedIndex == index,
                        onFocused = { focused -> if (focused) focusedIndex = index },
                        onNavigate = onNavigate,
                        focusRequester = if (index == selectedIndex) focusRequester else null,
                        onBoundsChanged = { rootX, w ->
                            bounds[index] = with(density) { (rootX - rowOriginX).toDp() } to w
                        },
                        density = density
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassNavPillItem(
    item: GlassNavItem,
    isSelected: Boolean,
    isFocused: Boolean,
    onFocused: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    focusRequester: FocusRequester?,
    /** Reports the item's x in root coordinates plus its measured width. */
    onBoundsChanged: (Float, Dp) -> Unit,
    density: androidx.compose.ui.unit.Density
) {
    val tokens = NuvioGlass.tokens
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) tokens.focusScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navItemScale"
    )
    // Focus inverts the chip to near-white, so its contents flip dark to stay legible.
    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFF101013)
            isSelected -> Color.White
            else -> Color.White.copy(alpha = 0.62f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navItemContent"
    )

    val showLabel = isFocused || isSelected
    val labelAlpha by animateFloatAsState(
        targetValue = if (showLabel) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navItemLabelAlpha"
    )

    Row(
        modifier = Modifier
            .height(tokens.pillHeight - 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onGloballyPositioned { coords ->
                val w = with(density) { coords.size.width.toDp() }
                onBoundsChanged(coords.positionInRoot().x, w)
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocused(it.isFocused) }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onNavigate(item.route) }
            )
            .padding(horizontal = tokens.pillItemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier.size(NuvioTheme.sizes.icons.md)
        when {
            // Drawer icons are raw SVG resources, not drawables, so painterResource cannot
            // load them — it throws casting the result to a BitmapDrawable.
            item.iconRes != null -> Icon(
                painter = rememberRawSvgPainter(item.iconRes, NuvioTheme.sizes.icons.md),
                contentDescription = item.label,
                tint = contentColor,
                modifier = iconModifier
            )
            item.icon != null -> Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = iconModifier
            )
        }
        if (showLabel) {
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}
