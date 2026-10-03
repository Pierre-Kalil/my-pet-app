package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.PrimaryDestination
import com.example.ui.theme.MeuPetDimensions

private val primaryDestinations = listOf(
    PrimaryDestination.Home,
    PrimaryDestination.Profile,
    PrimaryDestination.History
)

/** Navigation that uses three primary destinations in a bottom bar or rail. */
@Composable
fun AppScaffold(
    selectedDestination: PrimaryDestination?,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    showNavigation: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < MeuPetDimensions.mediumBreakpoint.value

    if (isCompact) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            // Each destination owns its safe-drawing insets. This avoids applying them twice
            // when a destination itself uses Material3 Scaffold.
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showNavigation) {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        primaryDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = selectedDestination == destination,
                                onClick = { onDestinationSelected(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) },
                                modifier = Modifier.sizeIn(
                                    minWidth = MeuPetDimensions.interactiveMinimum,
                                    minHeight = MeuPetDimensions.interactiveMinimum
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content()
            }
        }
    } else {
        if (showNavigation) {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Top
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                ) {
                    primaryDestinations.forEach { destination ->
                        NavigationRailItem(
                            selected = selectedDestination == destination,
                            onClick = { onDestinationSelected(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                            modifier = Modifier.sizeIn(
                                minWidth = MeuPetDimensions.interactiveMinimum,
                                minHeight = MeuPetDimensions.interactiveMinimum
                            )
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    content()
                }
            }
        } else {
            Box(modifier = modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

private val PrimaryDestination.icon
    get() = when (this) {
        PrimaryDestination.Home -> Icons.Default.Home
        PrimaryDestination.Profile -> Icons.Default.Person
        PrimaryDestination.History -> Icons.Default.History
    }
