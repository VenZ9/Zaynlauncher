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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zayn.launcher.data.Settings
import com.zayn.launcher.data.Store

@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onChanged: () -> Unit
) {
    var pickRam by remember { mutableStateOf(false) }
    var pickRenderer by remember { mutableStateOf(false) }
    var pickLayout by remember { mutableStateOf(false) }
    var editDir by remember { mutableStateOf(false) }
    var dirText by remember { mutableStateOf(settings.gameDir) }

    val layoutOptions = Store.layouts.map { it.name }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        SectionCard("Performance") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabelValue("RAM allocation", "${settings.ramMb} MB", Modifier.weight(1f))
                OutlinedButton(onClick = { pickRam = true }) { Text("Change") }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabelValue("Renderer", settings.renderer, Modifier.weight(1f))
                OutlinedButton(onClick = { pickRenderer = true }) { Text("Change") }
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard("Storage") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabelValue("Game directory", settings.gameDir, Modifier.weight(1f))
                OutlinedButton(onClick = { dirText = settings.gameDir; editDir = true }) { Text("Edit") }
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard("Controls") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabelValue(
                    "Default control layout",
                    Store.layouts.firstOrNull { it.id == settings.defaultLayoutId }?.name ?: layoutOptions.firstOrNull() ?: "-",
                    Modifier.weight(1f)
                )
                OutlinedButton(onClick = { pickLayout = true }) { Text("Change") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { Store.resetLayouts(); onChanged() }) { Text("Reset all control layouts") }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard("Appearance") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Dark theme", modifier = Modifier.weight(1f))
                Switch(checked = settings.darkTheme, onCheckedChange = { onSettingsChange(settings.copy(darkTheme = it)) })
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { Store.resetAll(); onSettingsChange(Store.settings); onChanged() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset launcher settings") }
    }

    if (pickRam) PickerDialog(
        "RAM allocation",
        Settings.RAM_OPTIONS.map { "$it MB" },
        "${settings.ramMb} MB",
        { onSettingsChange(settings.copy(ramMb = it.substringBefore(" ").toInt())) }
    ) { pickRam = false }

    if (pickRenderer) PickerDialog("Renderer", Settings.RENDERERS, settings.renderer, { onSettingsChange(settings.copy(renderer = it)) }) { pickRenderer = false }

    if (pickLayout) PickerDialog("Default control layout", layoutOptions, "", { name ->
        Store.layouts.firstOrNull { it.name == name }?.let { onSettingsChange(settings.copy(defaultLayoutId = it.id)) }
    }) { pickLayout = false }

    if (editDir) AlertDialog(
        onDismissRequest = { editDir = false },
        title = { Text("Game directory") },
        text = { OutlinedTextField(value = dirText, onValueChange = { dirText = it }, label = { Text("Path") }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onSettingsChange(settings.copy(gameDir = dirText.trim())); editDir = false }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { editDir = false }) { Text("Cancel") } }
    )
}
