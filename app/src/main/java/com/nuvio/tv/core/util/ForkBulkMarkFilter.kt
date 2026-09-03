package com.nuvio.tv.core.util

import com.nuvio.tv.domain.model.Video

/**
 * [FORK] Bulk "mark watched" actions must never mark episodes that have not aired yet.
 *
 * Addon metadata (Cinemeta/TMDB) routinely lists announced future seasons and episodes.
 * Upstream's bulk-mark paths filter only on specials/`season > 0`, so marking a show or a
 * season as watched also wrote a completed entry for every unaired episode. When the new
 * season later aired it was already flagged fully watched.
 *
 * This mirrors the availability rules already used by [com.nuvio.tv.domain.model.Meta.watchableEpisodes]:
 * an episode counts as markable unless the addon explicitly says it is unavailable, or its
 * release date is known and still in the future. Unknown release dates stay markable so
 * addons that omit dates keep working.
 */
internal fun List<Video>.airedForBulkMark(): List<Video> = filter { video ->
    video.available != false && isEpisodeReleaseAired(video.released) != false
}
