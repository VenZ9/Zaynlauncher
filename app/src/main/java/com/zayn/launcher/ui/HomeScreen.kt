package com.zayn.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zayn.launcher.data.Settings
import com.zayn.launcher.data.Store
import com.zayn.launcher.data.Versions
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    settings: Settings,
    version: String,
    onVersionChange: (String) -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onGoProfiles: () -> Unit
) {
    val profile = Store.activeProfile
    val layout = Store.activeLayout
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pickVersion by remember { mutableStateOf(false) }
    var pickRam by remember { mutableStateOf(false) }
    var pickRenderer by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("ZaynLauncher", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Offline Minecraft Java launcher",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        SectionCard("Active profile") {
            LabelValue("Username", profile?.username ?: "No profile")
            Spacer(Modifier.height(6.dp))
            LabelValue("UUID", profile?.uuid ?: "-")
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onGoProfiles) { Text("Manage profiles") }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard("Launch configuration") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelValue("Version", version, Modifier.weight(1f))
                LabelValue("RAM", "${settings.ramMb} MB", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            LabelValue("Renderer", settings.renderer)
            Spacer(Modifier.height(8.dp))
            LabelValue("Control layout", layout.name)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickVersion = true }, modifier = Modifier.weight(1f)) { Text("Version") }
                OutlinedButton(onClick = { pickRam = true }, modifier = Modifier.weight(1f)) { Text("RAM") }
                OutlinedButton(onClick = { pickRenderer = true }, modifier = Modifier.weight(1f)) { Text("Renderer") }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch { snackbar.showSnackbar("Minecraft runtime not connected yet") }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("  LAUNCH  $version", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "The Minecraft Java runtime is not bundled in this build yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        SnackbarHost(snackbar)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Runtime integration is a later milestone.", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (pickVersion) PickerDialog("Minecraft version", Versions.ALL, version, onVersionChange) { pickVersion = false }
    if (pickRam) PickerDialog(
        "RAM allocation",
        Settings.RAM_OPTIONS.map { "$it MB" },
        "${settings.ramMb} MB",
        { choice -> onSettingsChange(settings.copy(ramMb = choice.substringBefore(" ").toInt())) }
    ) { pickRam = false }
    if (pickRenderer) PickerDialog("Renderer", Settings.RENDERERS, settings.renderer, { onSettingsChange(settings.copy(renderer = it)) }) { pickRenderer = false }
}
