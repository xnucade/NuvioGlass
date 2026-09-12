package com.nuvio.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nuvio.tv.ui.components.glass.GlassClockPill
import com.nuvio.tv.ui.components.glass.GlassNavItem
import com.nuvio.tv.ui.components.glass.GlassNavPill
import com.nuvio.tv.ui.components.glass.LocalGlassChromeReveal
import com.nuvio.tv.ui.components.glass.LocalGlassHazeState
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.theme.NuvioGlass
import com.nuvio.tv.ui.theme.NuvioTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay

/** How long the chrome lingers after it loses focus before sliding away. */
private const val CHROME_AUTO_HIDE_MS = 3500L

/** Shorter timeout once the bar has been used, so it clears out of the way promptly. */
private const val CHROME_DISMISS_MS = 250L

/** How far the bar travels off the top edge when hidden. */
private val ChromeHiddenOffset = 120.dp

/**
 * App chrome for the Glass layout: no sidebar, a frosted nav pill and clock floating at the top.
 *
 * **Why the chrome hides itself.** A top bar over a full-bleed hero will always cover the title
 * treatment, and a TV hero puts its title high on the left. Rather than shrink the hero, the chrome
 * gets out of the way: it shows on arrival and whenever it holds focus, then slides up and fades
 * once you move down into the rows. Pressing up from the top of the content brings it back and
 * hands it focus, which is the same gesture people already use on every other TV app.
 *
 * That also means the blur is recomputed only while the chrome is on screen, which is the cheap
 * state for hardware like the Onn.
 */
@Composable
internal fun GlassScaffold(
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }

    val onRootRoute = currentRoute in rootRoutes
    var chromeVisible by remember { mutableStateOf(true) }
    var chromeFocused by remember { mutableStateOf(false) }
    // Once the bar has been driven, dropping back into content should dismiss it quickly
    // rather than making the user wait out the full arrival timeout.
    var chromeHasHeldFocus by remember { mutableStateOf(false) }
    var pendingNavFocus by remember { mutableStateOf(false) }

    val navItems = remember(drawerItems) {
        drawerItems.map { GlassNavItem(it.route, it.label, it.icon, it.iconRes) }
    }

    // Leaving a root route takes the chrome with it; arriving on one brings it back.
    LaunchedEffect(onRootRoute, currentRoute) {
        chromeVisible = onRootRoute
    }

    // Focus arriving on the bar is what reveals it. Focus traversal decides when that happens,
    // so the bar can only appear from the top of the content, never from halfway down a page.
    LaunchedEffect(chromeFocused) {
        if (chromeFocused) chromeVisible = true
    }

    // Focus can only land once the bar has animated back into view.
    LaunchedEffect(pendingNavFocus, chromeVisible) {
        if (!pendingNavFocus || !chromeVisible) return@LaunchedEffect
        delay(80)
        runCatching { navFocusRequester.requestFocus() }
        pendingNavFocus = false
    }

    // Idle chrome slides away on its own. It lingers on arrival so you can see where you are,
    // but leaves promptly once you have deliberately moved down into the content.
    val chromeAutoHides = currentRoute == Screen.Home.route
    LaunchedEffect(chromeVisible, chromeFocused, currentRoute, chromeAutoHides) {
        if (!chromeVisible || chromeFocused || !onRootRoute || !chromeAutoHides) return@LaunchedEffect
        delay(if (chromeHasHeldFocus) CHROME_DISMISS_MS else CHROME_AUTO_HIDE_MS)
        chromeVisible = false
    }

    BackHandler(enabled = onRootRoute && chromeFocused) {
        chromeVisible = false
        runCatching { contentFocusRequester.requestFocus() }
    }
    BackHandler(enabled = onRootRoute && !chromeFocused) {
        if (longPressBackHeld.value) return@BackHandler
        onExitApp()
    }

    // Handed to the content, which calls it only when Up is pressed with no row above.
    val revealChrome: () -> Boolean = {
        if (onRootRoute && !chromeFocused) {
            chromeVisible = true
            pendingNavFocus = true
            true
        } else {
            false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val contentDim by animateFloatAsState(
            targetValue = if (chromeFocused) 0.45f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "glassContentDim"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentDim }
                .hazeSource(state = hazeState)
        ) {
            CompositionLocalProvider(
                LocalSidebarExpanded provides false,
                LocalContentFocusRequester provides contentFocusRequester,
                LocalGlassChromeReveal provides revealChrome
            ) {
                NuvioNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    hideBuiltInHeaders = true
                )
            }
        }

        if (onRootRoute) {
            // The bar is ALWAYS composed on a root route, and hides by sliding out under a
            // graphicsLayer rather than leaving the composition. That is deliberate: a bar removed
            // from the tree is also removed from the focus graph, and then pressing Up has to be
            // intercepted by hand — which is exactly what made Up jump here from halfway down a
            // page. Kept in the graph, ordinary focus traversal reaches it only from the top row,
            // and graphicsLayer moves it without disturbing its focus bounds.
            val shown = chromeVisible
            val hiddenOffsetPx = with(LocalDensity.current) { ChromeHiddenOffset.toPx() }
            val chromeShift by animateFloatAsState(
                targetValue = if (shown) 0f else -hiddenOffsetPx,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "glassChromeShift"
            )
            val chromeAlpha by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "glassChromeAlpha"
            )

            CompositionLocalProvider(LocalGlassHazeState provides hazeState) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            translationY = chromeShift
                            alpha = chromeAlpha
                        }
                ) {
                    // A soft scrim under the chrome so white text survives a bright backdrop,
                    // fading to nothing well before the hero title starts.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = NuvioGlass.tokens.scrimAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
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
                            items = navItems,
                            selectedRoute = selectedDrawerRoute ?: currentRoute,
                            onNavigate = { route ->
                                chromeVisible = false
                                // onNavigate only drives the optimistic highlight; the back-stack
                                // move is the sidebar's own helper, so behaviour stays identical.
                                onNavigate(route)
                                navigateToDrawerRoute(navController, currentRoute, route)
                            },
                            focusRequester = navFocusRequester,
                            onFocusChanged = { hasFocus ->
                                chromeFocused = hasFocus
                                if (hasFocus) chromeHasHeldFocus = true
                            }
                        )
                        GlassClockPill()
                    }
                }
            }
        }
    }
}
