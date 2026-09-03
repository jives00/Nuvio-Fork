package com.nuvio.tv.core.util

import com.nuvio.tv.domain.model.Video
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/** [FORK] Guards the unaired-episode filter applied to bulk "mark watched" actions. */
class ForkBulkMarkFilterTest {

    private fun video(
        season: Int,
        episode: Int,
        released: String? = null,
        available: Boolean? = null
    ) = Video(
        id = "tt1:$season:$episode",
        title = "S${season}E$episode",
        released = released,
        thumbnail = null,
        season = season,
        episode = episode,
        overview = null,
        available = available
    )

    private fun daysFromNow(days: Long): String = DateTimeFormatter.ISO_INSTANT
        .format(Instant.now().plusSeconds(days * 24 * 60 * 60).atZone(ZoneOffset.UTC).toInstant())

    @Test
    fun `keeps episodes with no release date`() {
        val episodes = listOf(video(1, 1), video(1, 2))
        assertEquals(episodes, episodes.airedForBulkMark())
    }

    @Test
    fun `keeps already aired episodes`() {
        val episodes = listOf(video(1, 1, released = daysFromNow(-30)))
        assertEquals(episodes, episodes.airedForBulkMark())
    }

    @Test
    fun `drops announced future season`() {
        val aired = video(1, 1, released = daysFromNow(-30))
        val upcoming = listOf(video(2, 1, released = daysFromNow(30)), video(2, 2, released = daysFromNow(37)))
        assertEquals(listOf(aired), (listOf(aired) + upcoming).airedForBulkMark())
    }

    @Test
    fun `drops episodes the addon marks unavailable`() {
        val aired = video(1, 1, released = daysFromNow(-30))
        val unavailable = video(1, 2, released = daysFromNow(-1), available = false)
        assertEquals(listOf(aired), listOf(aired, unavailable).airedForBulkMark())
    }

    @Test
    fun `keeps episodes explicitly marked available`() {
        val episodes = listOf(video(1, 1, released = daysFromNow(-1), available = true))
        assertEquals(episodes, episodes.airedForBulkMark())
    }
}
