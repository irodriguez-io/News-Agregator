package io.irodriguez.intentionalreading.ui.format

import io.irodriguez.intentionalreading.ui.screens.history.HistoryPeriod
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object RelativeTime {
    fun relativeDate(
        value: Instant?,
        now: Instant,
        zone: ZoneId,
        locale: Locale,
    ): String {
        if (value == null) return ""
        val deltaMillis = Duration.between(value, now).toMillis().coerceAtLeast(0)
        val hours = deltaMillis / MILLIS_PER_HOUR
        if (hours < 1) return "Now"
        if (hours < 24) return "${hours}h"
        val days = hours / 24
        if (days < 31) return "${days}d"
        return DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
            .withZone(zone)
            .format(value)
    }

    fun historyGroup(value: Instant?, now: Instant, zone: ZoneId): HistoryPeriod {
        if (value == null) return HistoryPeriod.EARLIER
        val valueDate = value.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        return when (ChronoUnit.DAYS.between(valueDate, today)) {
            0L -> HistoryPeriod.TODAY
            1L -> HistoryPeriod.YESTERDAY
            else -> HistoryPeriod.EARLIER
        }
    }

    fun localDateTime(value: Instant?, zone: ZoneId, locale: Locale): String {
        if (value == null) return ""
        return DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", locale)
            .withZone(zone)
            .format(value)
    }

    fun readingTime(value: Int?): String = if (value != null && value > 0) "~$value min" else ""

    private const val MILLIS_PER_HOUR = 3_600_000L
}
