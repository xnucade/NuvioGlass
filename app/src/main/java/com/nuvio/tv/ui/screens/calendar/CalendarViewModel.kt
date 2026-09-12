package com.nuvio.tv.ui.screens.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.util.parseEpisodeReleaseLocalDate
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.repository.LibraryRepository
import com.nuvio.tv.domain.repository.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import javax.inject.Inject

/** Days of already-aired episodes kept on screen, so you can catch up on what you missed. */
private const val DAYS_BEHIND = 7L

/** How far ahead the schedule runs. A month covers a typical weekly season's remaining run. */
private const val DAYS_AHEAD = 28L

/**
 * Addons answer one series at a time and a large library would otherwise fire dozens of parallel
 * requests at the same host. Six in flight keeps it responsive without hammering the addon.
 */
private const val META_CONCURRENCY = 6

@Immutable
data class CalendarEpisode(
    val seriesId: String,
    val seriesType: String,
    val seriesName: String,
    val addonBaseUrl: String?,
    val poster: String?,
    val thumbnail: String?,
    val season: Int,
    val episode: Int,
    val title: String,
    val overview: String?,
    val date: LocalDate
) {
    /** "S2E7", the shorthand every TV UI uses. */
    val code: String get() = "S${season}E${episode}"
}

@Immutable
data class CalendarDay(
    val date: LocalDate,
    val episodes: List<CalendarEpisode>
)

@Immutable
data class CalendarUiState(
    val isLoading: Boolean = true,
    val days: List<CalendarDay> = emptyList(),
    val trackedSeriesCount: Int = 0,
    val hasLibrarySeries: Boolean = true
)

/**
 * Builds an airing schedule out of the series already in the library.
 *
 * There is no calendar endpoint in the Stremio addon protocol, so the schedule is derived: take
 * every series the user follows, ask the addons for its meta, and bucket the episode release dates
 * that fall inside the window. That means the calendar is exactly as complete as the metadata
 * addons are, and a series whose addon omits air dates simply will not appear.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val metaRepository: MetaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.libraryItems
                .map { entries -> entries.filter { it.type.equals("series", ignoreCase = true) } }
                .distinctUntilChanged { old, new -> old.map(LibraryEntry::id) == new.map(LibraryEntry::id) }
                .collect { series -> rebuild(series) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            libraryRepository.refreshNow()
        }
    }

    private suspend fun rebuild(series: List<LibraryEntry>) {
        if (series.isEmpty()) {
            _uiState.value = CalendarUiState(
                isLoading = false,
                days = emptyList(),
                trackedSeriesCount = 0,
                hasLibrarySeries = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            trackedSeriesCount = series.size,
            hasLibrarySeries = true
        )

        val today = LocalDate.now()
        val from = today.minusDays(DAYS_BEHIND)
        val to = today.plusDays(DAYS_AHEAD)

        val episodes = withContext(Dispatchers.Default) {
            val gate = Semaphore(META_CONCURRENCY)
            coroutineScope {
                series.map { entry ->
                    async {
                        gate.withPermit { episodesFor(entry, from, to) }
                    }
                }.awaitAll().flatten()
            }
        }

        val days = episodes
            .groupBy { it.date }
            .toSortedMap()
            .map { (date, items) ->
                CalendarDay(
                    date = date,
                    episodes = items.sortedWith(
                        compareBy({ it.seriesName }, { it.season }, { it.episode })
                    )
                )
            }

        _uiState.value = CalendarUiState(
            isLoading = false,
            days = days,
            trackedSeriesCount = series.size,
            hasLibrarySeries = true
        )
    }

    private suspend fun episodesFor(
        entry: LibraryEntry,
        from: LocalDate,
        to: LocalDate
    ): List<CalendarEpisode> {
        val meta = resolveMeta(entry) ?: return emptyList()
        return meta.videos.mapNotNull { video ->
            val season = video.season ?: return@mapNotNull null
            val episode = video.episode ?: return@mapNotNull null
            // Season 0 is specials; they have erratic dates and would clutter the schedule.
            if (season <= 0) return@mapNotNull null
            val date = parseEpisodeReleaseLocalDate(video.released) ?: return@mapNotNull null
            if (date < from || date > to) return@mapNotNull null

            CalendarEpisode(
                seriesId = entry.id,
                seriesType = entry.type,
                seriesName = meta.name.ifBlank { entry.name },
                addonBaseUrl = entry.addonBaseUrl,
                poster = meta.poster ?: entry.poster,
                thumbnail = video.thumbnail ?: meta.backdropUrl,
                season = season,
                episode = episode,
                title = video.title.ifBlank { "Episode $episode" },
                overview = video.overview,
                date = date
            )
        }
    }

    private suspend fun resolveMeta(entry: LibraryEntry): Meta? {
        metaRepository.getCachedMeta(entry.type, entry.id)?.let { return it }
        val result = metaRepository
            .getMetaFromAllAddons(type = entry.type, id = entry.id)
            .firstOrNull { it !is NetworkResult.Loading }
        return (result as? NetworkResult.Success)?.data
    }
}
