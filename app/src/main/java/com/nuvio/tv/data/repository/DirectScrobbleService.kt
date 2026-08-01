package com.nuvio.tv.data.repository

import android.util.Log
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.tracking.TrackingMediaKind
import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.data.remote.api.DirectScrobbleApi
import com.nuvio.tv.data.remote.dto.trakt.TraktEpisodeDto
import com.nuvio.tv.data.remote.dto.trakt.TraktIdsDto
import com.nuvio.tv.data.remote.dto.trakt.TraktMovieDto
import com.nuvio.tv.data.remote.dto.trakt.TraktScrobbleRequestDto
import com.nuvio.tv.data.remote.dto.trakt.TraktShowDto
import javax.inject.Inject
import javax.inject.Singleton

// [FORK] Direct scrobble service — sends to custom endpoint, no Trakt auth required.
// Deliberately NOT registered as a TrackingProvider: TrackingProviderId is a closed
// enum that drives the tracking settings UI, and this endpoint is always-on rather
// than something the user connects. Call sites invoke it alongside the coordinator.
@Singleton
class DirectScrobbleService @Inject constructor(
    private val api: DirectScrobbleApi,
    private val episodeMappingService: TraktEpisodeMappingService
) {
    companion object {
        private const val TAG = "DirectScrobbleSvc"
    }

    suspend fun start(media: TrackingMediaReference, progressPercent: Float) {
        if (BuildConfig.SCROBBLE_API_URL.isBlank()) return
        val item = media.toScrobbleItem() ?: return
        runCatching {
            api.scrobbleStart(BuildConfig.SCROBBLE_API_KEY, buildBody(item, progressPercent, paused = false))
        }.onFailure { Log.w(TAG, "scrobble start failed", it) }
    }

    suspend fun stop(media: TrackingMediaReference, progressPercent: Float, paused: Boolean) {
        if (BuildConfig.SCROBBLE_API_URL.isBlank()) return
        val item = media.toScrobbleItem() ?: return
        runCatching {
            api.scrobbleStop(BuildConfig.SCROBBLE_API_KEY, buildBody(item, progressPercent, paused))
        }.onFailure { Log.w(TAG, "scrobble stop failed", it) }
    }

    // Mirrors TraktTrackingScrobbler's conversion so the direct endpoint receives the
    // same season/episode numbers (post TVDB mapping) that Trakt would.
    private suspend fun TrackingMediaReference.toScrobbleItem(): TraktScrobbleItem? {
        val traktIds = TraktIdsDto(
            trakt = ids.trakt.toIntExactOrNull(),
            imdb = ids.imdb?.takeIf(String::isNotBlank),
            tmdb = ids.tmdb.toIntExactOrNull(),
            tvdb = ids.tvdb?.toIntOrNull()
        )
        if (!traktIds.hasAnyId()) return null
        if (kind == TrackingMediaKind.MOVIE) {
            return TraktScrobbleItem.Movie(title = title, year = year, ids = traktIds)
        }

        val episodeReference = episode ?: return null
        val season = episodeReference.season ?: return null
        val contentId = catalog?.contentId ?: ids.imdb ?: ids.tmdb?.let { "tmdb:$it" }
        val mapped = episodeMappingService.resolveEpisodeMapping(
            contentId = contentId,
            contentType = catalog?.contentType ?: "series",
            videoId = catalog?.videoId,
            season = season,
            episode = episodeReference.number,
            episodeTitle = episodeReference.title
        )
        return TraktScrobbleItem.Episode(
            showTitle = title,
            showYear = year,
            showIds = traktIds,
            season = mapped?.season ?: season,
            number = mapped?.episode ?: episodeReference.number,
            episodeTitle = episodeReference.title
        )
    }

    private fun buildBody(item: TraktScrobbleItem, progressPercent: Float, paused: Boolean): TraktScrobbleRequestDto =
        when (item) {
            is TraktScrobbleItem.Movie -> TraktScrobbleRequestDto(
                movie = TraktMovieDto(title = item.title, year = item.year, ids = item.ids),
                progress = progressPercent.coerceIn(0f, 100f),
                paused = paused
            )
            is TraktScrobbleItem.Episode -> TraktScrobbleRequestDto(
                show = TraktShowDto(title = item.showTitle, year = item.showYear, ids = item.showIds),
                episode = TraktEpisodeDto(
                    title = item.episodeTitle,
                    season = item.season,
                    number = item.number
                ),
                progress = progressPercent.coerceIn(0f, 100f),
                paused = paused
            )
        }
}

private fun Long?.toIntExactOrNull(): Int? = this?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
