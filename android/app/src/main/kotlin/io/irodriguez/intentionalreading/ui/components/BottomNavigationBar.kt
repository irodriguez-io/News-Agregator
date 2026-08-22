package io.irodriguez.intentionalreading.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.ui.Destination
import io.irodriguez.intentionalreading.ui.NavigationCounts
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun BottomNavigationBar(
    destination: Destination,
    counts: NavigationCounts,
    onDestinationSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    NavigationBar(
        modifier = modifier.heightIn(min = 54.dp),
        containerColor = tokens.surface,
        contentColor = tokens.fg,
        tonalElevation = 0.dp,
    ) {
        NavigationDestination(
            destination = Destination.READ_LATER,
            selectedDestination = destination,
            icon = R.drawable.ic_read_later,
            label = R.string.read_later,
            count = counts.readLater,
            onSelected = onDestinationSelected,
        )
        NavigationDestination(
            destination = Destination.DISCOVER,
            selectedDestination = destination,
            icon = R.drawable.ic_discover,
            label = R.string.discover,
            count = null,
            elevated = true,
            onSelected = onDestinationSelected,
        )
        NavigationDestination(
            destination = Destination.HISTORY,
            selectedDestination = destination,
            icon = R.drawable.ic_history,
            label = R.string.history,
            count = counts.history,
            onSelected = onDestinationSelected,
        )
    }
}

@Composable
private fun RowScope.NavigationDestination(
    destination: Destination,
    selectedDestination: Destination,
    @DrawableRes icon: Int,
    @StringRes label: Int,
    count: Int?,
    onSelected: (Destination) -> Unit,
    elevated: Boolean = false,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val labelText = stringResource(label)
    NavigationBarItem(
        selected = destination == selectedDestination,
        onClick = { onSelected(destination) },
        icon = {
            val iconContent: @Composable () -> Unit = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = labelText,
                    modifier = if (elevated) Modifier.offset(y = (-7).dp) else Modifier,
                )
            }
            if (count == null) {
                iconContent()
            } else {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = tokens.fg,
                            contentColor = tokens.surface,
                        ) {
                            Text(count.coerceAtLeast(0).toString())
                        }
                    },
                    content = { iconContent() },
                )
            }
        },
        label = { Text(labelText) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = tokens.surface,
            selectedTextColor = tokens.fg,
            indicatorColor = tokens.fg,
            unselectedIconColor = tokens.muted,
            unselectedTextColor = tokens.muted,
            disabledIconColor = tokens.muted.copy(alpha = 0.38f),
            disabledTextColor = tokens.muted.copy(alpha = 0.38f),
        ),
    )
}
