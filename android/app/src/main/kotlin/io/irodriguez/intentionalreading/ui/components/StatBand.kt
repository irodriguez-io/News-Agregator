package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
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
    val shapes = LocalIntentionalReadingShapes.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.statBand,
        color = tokens.container,
        contentColor = tokens.fg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            StatCell(stats[0], Modifier.weight(1f))
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = tokens.outlineVariant,
            )
            StatCell(stats[1], Modifier.weight(1f))
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = tokens.outlineVariant,
            )
            StatCell(stats[2], Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(stat: StatItem, modifier: Modifier = Modifier) {
    val tokens = LocalIntentionalReadingTokens.current
    val spacing = LocalIntentionalReadingSpacing.current
    Box(modifier = modifier.padding(spacing.gutter)) {
        Column {
            Text(
                text = stat.label.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium,
                color = tokens.muted,
            )
            Text(
                text = stat.value,
                style = MaterialTheme.typography.displayMedium,
                color = tokens.fg,
            )
        }
    }
}

internal fun knownReadingTimeValue(minutes: Int): String =
    if (minutes > 0) "~$minutes min" else "Unavailable"

internal fun availableStatValue(value: String?): String = value?.takeIf { it.isNotEmpty() } ?: "Unavailable"
