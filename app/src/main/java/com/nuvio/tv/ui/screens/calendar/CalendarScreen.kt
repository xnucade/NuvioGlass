package com.nuvio.tv.ui.screens.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.glass.GlassBadge
import com.nuvio.tv.ui.components.glass.GlassPanel
import com.nuvio.tv.ui.theme.NuvioGlass
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val CardWidth = 260.dp

/**
 * An airing schedule for the series in your library, newest day first within a rolling window.
 *
 * Episodes that have already aired are dimmed rather than hidden, so the screen doubles as a
 * "what did I miss" list instead of only looking forward.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.days.isEmpty() -> CalendarMessage(
                title = stringResource(R.string.calendar_loading_title),
                body = stringResource(R.string.calendar_loading_body)
            )

            !uiState.hasLibrarySeries -> CalendarMessage(
                title = stringResource(R.string.calendar_empty_library_title),
                body = stringResource(R.string.calendar_empty_library_body)
            )

            uiState.days.isEmpty() -> CalendarMessage(
                title = stringResource(R.string.calendar_empty_title),
                body = stringResource(R.string.calendar_empty_body)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = NuvioTheme.spacing.screen.overscanHorizontal,
                    end = NuvioTheme.spacing.screen.overscanHorizontal,
                    top = 110.dp,
                    bottom = NuvioTheme.spacing.rail.tailPadding
                ),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xl)
            ) {
                items(uiState.days, key = { it.date.toString() }) { day ->
                    CalendarDaySection(day = day, today = today, onNavigateToDetail = onNavigateToDetail)
                }
            }
        }
    }
}

@Composable
private fun CalendarDaySection(
    day: CalendarDay,
    today: LocalDate,
    onNavigateToDetail: (String, String, String) -> Unit
) {
    val locale = LocalConfiguration.current.let { config ->
        androidx.core.os.ConfigurationCompat.getLocales(config)[0] ?: Locale.getDefault()
    }
    val headerFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }

    Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
        ) {
            Text(
                text = day.date.format(headerFormatter),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            relativeLabel(day.date, today)?.let { label ->
                GlassBadge(text = label, flat = true)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
        ) {
            items(day.episodes, key = { "${it.seriesId}:${it.season}:${it.episode}" }) { episode ->
                CalendarEpisodeCard(
                    episode = episode,
                    hasAired = !episode.date.isAfter(today),
                    onClick = {
                        onNavigateToDetail(episode.seriesId, episode.seriesType, episode.addonBaseUrl.orEmpty())
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarEpisodeCard(
    episode: CalendarEpisode,
    hasAired: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(NuvioRadii.tokens.xl)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) NuvioGlass.tokens.cardFocusScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "calendarCardScale"
    )
    // Aired episodes recede, so the eye lands on what is coming up next.
    val restingAlpha = if (hasAired) 0.55f else 1f
    val contentAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else restingAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "calendarCardAlpha"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "calendarCardBorder"
    )

    Column(
        modifier = Modifier
            .width(CardWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = contentAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .background(NuvioTheme.colors.BackgroundCard)
                .border(width = NuvioTheme.strokes.medium, color = borderColor, shape = shape)
        ) {
            AsyncImage(
                model = episode.thumbnail ?: episode.poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Keeps the episode code readable over any thumbnail.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.75f)
                        )
                    )
            )
            GlassBadge(
                text = episode.code,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(NuvioTheme.spacing.sm)
            )
        }

        Text(
            text = episode.seriesName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = episode.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CalendarMessage(title: String, body: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(
            shape = RoundedCornerShape(NuvioRadii.tokens.panel),
            modifier = Modifier.width(560.dp)
        ) {
            Column(
                modifier = Modifier.padding(NuvioTheme.spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f)
                )
            }
        }
    }
}

/** "Today", "Tomorrow" or "in 3 days" — the part a bare date makes you work out yourself. */
@Composable
private fun relativeLabel(date: LocalDate, today: LocalDate): String? {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> stringResource(R.string.calendar_today)
        days == 1L -> stringResource(R.string.calendar_tomorrow)
        days == -1L -> stringResource(R.string.calendar_yesterday)
        else -> null
    }
}
