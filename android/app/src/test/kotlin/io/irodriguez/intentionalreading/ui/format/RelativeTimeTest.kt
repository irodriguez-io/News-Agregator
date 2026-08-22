package io.irodriguez.intentionalreading.ui.format

import io.irodriguez.intentionalreading.ui.screens.history.HistoryPeriod
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {
    @Test
    fun `relative date uses Now for less than one hour`() {
        assertEquals("Now", relative(now.minusSeconds(3_599)))
    }

    @Test
    fun `relative date uses floored hours below twenty four hours`() {
        assertEquals("1h", relative(now.minusSeconds(3_600)))
        assertEquals("23h", relative(now.minusSeconds(86_399)))
    }

    @Test
    fun `relative date uses floored days below thirty one days`() {
        assertEquals("1d", relative(now.minusSeconds(86_400)))
        assertEquals("30d", relative(now.minusSeconds(30L * 86_400 + 86_399)))
    }

    @Test
    fun `relative date switches to localized absolute date at thirty one days`() {
        val published = now.minusSeconds(31L * 86_400)

        assertEquals("Jul 22, 2026", relative(published))
    }

    @Test
    fun `relative date clamps a future publication delta to Now`() {
        assertEquals("Now", relative(now.plusSeconds(86_400)))
    }

    @Test
    fun `relative date omits an absent publication instant`() {
        assertEquals("", relative(null))
    }

    @Test
    fun `history group compares local calendar dates rather than elapsed hours`() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val localNow = Instant.parse("2026-08-22T07:30:00Z")

        assertEquals(
            HistoryPeriod.TODAY,
            RelativeTime.historyGroup(Instant.parse("2026-08-22T07:05:00Z"), localNow, losAngeles),
        )
        assertEquals(
            HistoryPeriod.YESTERDAY,
            RelativeTime.historyGroup(Instant.parse("2026-08-22T06:55:00Z"), localNow, losAngeles),
        )
        assertEquals(
            HistoryPeriod.EARLIER,
            RelativeTime.historyGroup(Instant.parse("2026-08-20T07:30:00Z"), localNow, losAngeles),
        )
        assertEquals(HistoryPeriod.EARLIER, RelativeTime.historyGroup(null, localNow, losAngeles))
    }

    @Test
    fun `reading time renders only a positive integer`() {
        assertEquals("~7 min", RelativeTime.readingTime(7))
        assertEquals("", RelativeTime.readingTime(null))
        assertEquals("", RelativeTime.readingTime(0))
        assertEquals("", RelativeTime.readingTime(-1))
    }

    private fun relative(value: Instant?): String = RelativeTime.relativeDate(
        value = value,
        now = now,
        zone = zone,
        locale = Locale.US,
    )

    private companion object {
        val now: Instant = Instant.parse("2026-08-22T12:00:00Z")
        val zone: ZoneId = ZoneId.of("UTC")
    }
}
