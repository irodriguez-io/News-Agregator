package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

data class StatItem(
    val label: String,
    val value: String,
)

@Composable
fun StatBand(stats: List<StatItem>, modifier: Modifier = Modifier) {
    require(stats.size == 3)
    val tokens = LocalIntentionalReadingTokens.current
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 2.dp, color = tokens.strongBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            StatCell(stats[0], Modifier.weight(1f))
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = tokens.border,
            )
            StatCell(stats[1], Modifier.weight(1f))
        }
        HorizontalDivider(color = tokens.border)
        StatCell(stats[2], Modifier.fillMaxWidth())
        HorizontalDivider(color = tokens.border)
    }
}

@Composable
private fun StatCell(stat: StatItem, modifier: Modifier = Modifier) {
    val tokens = LocalIntentionalReadingTokens.current
    Box(modifier = modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
        Column {
            Text(
                text = stat.label.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.muted,
            )
            Text(
                text = stat.value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 25.sp,
                ),
                color = tokens.fg,
            )
        }
    }
}

internal fun knownReadingTimeValue(minutes: Int): String =
    if (minutes > 0) "~$minutes min" else "Unavailable"

internal fun availableStatValue(value: String?): String = value?.takeIf { it.isNotEmpty() } ?: "Unavailable"
