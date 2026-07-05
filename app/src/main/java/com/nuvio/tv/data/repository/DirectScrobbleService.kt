package com.nuvio.tv.data.repository

import android.util.Log
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.DirectScrobbleApi
import com.nuvio.tv.data.remote.dto.trakt.TraktEpisodeDto
import com.nuvio.tv.data.remote.dto.trakt.TraktMovieDto
import com.nuvio.tv.data.remote.dto.trakt.TraktScrobbleRequestDto
import com.nuvio.tv.data.remote.dto.trakt.TraktShowDto
import javax.inject.Inject
import javax.inject.Singleton

// [FORK] Direct scrobble service — sends to custom endpoint, no Trakt auth required
@Singleton
class DirectScrobbleService @Inject constructor(
    private val api: DirectScrobbleApi
) {
    companion object {
        private const val TAG = "DirectScrobbleSvc"
    }

    suspend fun start(item: TraktScrobbleItem, progressPercent: Float) {
        if (BuildConfig.SCROBBLE_API_URL.isBlank()) return
        runCatching {
            api.scrobbleStart(BuildConfig.SCROBBLE_API_KEY, buildBody(item, progressPercent, paused = false))
        }.onFailure { Log.w(TAG, "scrobble start failed", it) }
    }

    suspend fun stop(item: TraktScrobbleItem, progressPercent: Float, paused: Boolean) {
        if (BuildConfig.SCROBBLE_API_URL.isBlank()) return
        runCatching {
            api.scrobbleStop(BuildConfig.SCROBBLE_API_KEY, buildBody(item, progressPercent, paused))
        }.onFailure { Log.w(TAG, "scrobble stop failed", it) }
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
