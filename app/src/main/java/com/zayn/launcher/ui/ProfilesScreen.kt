package com.zayn.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.zayn.launcher.data.Profile
import com.zayn.launcher.data.Store

@Composable
fun ProfilesScreen(rev: Int, onChanged: () -> Unit) {
    var profiles by remember(rev) { mutableStateOf(Store.profiles) }
    var selected by remember(rev) { mutableStateOf(Store.selectedProfileId) }
    var showAdd by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Profile?>(null) }
    var newName by remember { mutableStateOf("") }

    fun reload() { profiles = Store.profiles; selected = Store.selectedProfileId; onChanged() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Profiles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { newName = ""; showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = null); Text(" New")
            }
        }
        Text(
            "Offline profiles are unlimited and stored on this device only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles, key = { it.id }) { p ->
                val isActive = p.id == selected
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { Store.selectedProfileId = p.id; reload() },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.username, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isActive) "Active profile" else p.uuid,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { newName = p.username; renameTarget = p }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = {
                            Store.profiles = Store.profiles.filterNot { it.id == p.id }
                            if (Store.selectedProfileId == p.id) Store.selectedProfileId = Store.profiles.firstOrNull()?.id ?: ""
                            reload()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AlertDialog(
        onDismissRequest = { showAdd = false },
        title = { Text("New offline profile") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Username") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val name = newName.trim()
                if (name.isNotEmpty()) {
                    val created = Profile.create(name)
                    Store.profiles = Store.profiles + created
                    Store.selectedProfileId = created.id
                    showAdd = false
                    reload()
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
    )

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename profile") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Username") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        Store.profiles = Store.profiles.map {
                            if (it.id == target.id) it.copy(username = name, uuid = Profile.offlineUuid(name)) else it
                        }
                        renameTarget = null
                        reload()
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }
}
