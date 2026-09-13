package com.zayn.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.zayn.launcher.data.Settings
import com.zayn.launcher.data.Store

enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    PROFILES("Profiles", Icons.Filled.Person),
    CONTROLS("Controls", Icons.Filled.Build),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var settings by remember { mutableStateOf(Store.settings) }
    var version by remember { mutableStateOf(Store.selectedVersion) }

    // Bumping this forces every screen to re-read from Store after a write.
    var rev by remember { mutableStateOf(0) }
    fun refresh() { rev++ }

    ZaynTheme(dark = settings.darkTheme) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Screen.entries.forEach { s ->
                        NavigationBarItem(
                            selected = screen == s,
                            onClick = { screen = s },
                            icon = { Icon(s.icon, contentDescription = s.label) },
                            label = { Text(s.label) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        settings = settings,
                        version = version,
                        onVersionChange = { version = it; Store.selectedVersion = it },
                        onSettingsChange = { settings = it; Store.settings = it; refresh() },
                        onGoProfiles = { screen = Screen.PROFILES }
                    )
                    Screen.PROFILES -> ProfilesScreen(rev = rev, onChanged = { refresh() })
                    Screen.CONTROLS -> ControlsScreen(rev = rev, onChanged = { refresh() })
                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsChange = { settings = it; Store.settings = it; refresh() },
                        onChanged = { refresh() }
                    )
                }
            }
        }
    }
}
