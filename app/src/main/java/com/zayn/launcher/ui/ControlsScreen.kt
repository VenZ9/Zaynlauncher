package com.zayn.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zayn.launcher.data.Control
import com.zayn.launcher.data.ControlAction
import com.zayn.launcher.data.Layout
import com.zayn.launcher.data.Store

@Composable
fun ControlsScreen(rev: Int, onChanged: () -> Unit) {
    var layoutId by remember(rev) { mutableStateOf(Store.activeLayout.id) }
    var controls by remember(rev) { mutableStateOf(Store.activeLayout.controls) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var boxPx by remember { mutableStateOf(IntSize.Zero) }

    var showLayoutPicker by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val layoutName = Store.layouts.firstOrNull { it.id == layoutId }?.name ?: "Custom"
    val selected = controls.firstOrNull { it.id == selectedId }

    fun save() {
        Store.layouts = Store.layouts.map { if (it.id == layoutId) it.copy(controls = controls) else it }
        onChanged()
    }

    fun replaceControl(updated: Control) {
        controls = controls.map { if (it.id == updated.id) updated else it }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Controls", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { showLayoutPicker = true }) { Text(layoutName) }
        }
        Text(
            "Drag to move, use the corner handle to resize.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))

        // ---- editing canvas ----
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF11161C), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .onSizeChanged { boxPx = it }
        ) {
            if (boxPx.width > 0) {
                controls.forEach { c ->
                    val move = remember(c.id) {
                        Modifier.pointerInput(c.id, boxPx.width, boxPx.height) {
                            detectDragGestures { change, delta ->
                                change.consume()
                                val nx = (c.x + delta.x / boxPx.width).coerceIn(0f, 1f - c.w)
                                val ny = (c.y + delta.y / boxPx.height).coerceIn(0f, 1f - c.h)
                                replaceControl(c.copy(x = nx, y = ny))
                            }
                        }
                    }
                    val resize = remember(c.id) {
                        Modifier.pointerInput(c.id, boxPx.width, boxPx.height) {
                            detectDragGestures { change, delta ->
                                change.consume()
                                val nw = (c.w + delta.x / boxPx.width).coerceIn(0.06f, 0.5f)
                                val nh = (c.h + delta.y / boxPx.height).coerceIn(0.06f, 0.5f)
                                replaceControl(c.copy(w = nw, h = nh))
                            }
                        }
                    }

                    Box(
                        Modifier
                            .offset(x = (c.x * boxPx.width).dp, y = (c.y * boxPx.height).dp)
                            .size((c.w * boxPx.width).dp, (c.h * boxPx.height).dp)
                            .alpha(if (c.enabled) c.opacity else 0.25f)
                            .background(
                                if (c.id == selectedId) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (c.id == selectedId) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedId = c.id }
                            .then(move)
                    ) {
                        Text(
                            c.action.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        // resize handle
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                                .then(resize)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- selected control properties ----
        selected?.let { c ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.action.label + "  (key ${c.action.key})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { controls = controls.filterNot { it.id == c.id }; selectedId = null }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete control")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(70.dp))
                        Slider(
                            value = c.opacity,
                            onValueChange = { replaceControl(c.copy(opacity = it)) },
                            valueRange = 0.2f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Switch(checked = c.enabled, onCheckedChange = { replaceControl(c.copy(enabled = it)) })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ---- action bar ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null); Text(" Add")
            }
            OutlinedButton(onClick = { renameText = layoutName; showRename = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Edit, contentDescription = null); Text(" Rename")
            }
            OutlinedButton(
                onClick = { controls = Layout.default().controls },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null); Text(" Reset")
            }
            OutlinedButton(onClick = { save() }, modifier = Modifier.weight(1f)) { Text("Save") }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showLayoutPicker = true }, modifier = Modifier.weight(1f)) { Text("Switch layout") }
            OutlinedButton(
                onClick = {
                    Store.layouts = Store.layouts.filterNot { it.id == layoutId }
                    if (Store.layouts.isEmpty()) Store.layouts = listOf(Layout.default())
                    Store.selectedLayoutId = Store.layouts.first().id
                    onChanged()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Delete layout") }
        }
    }

    if (showLayoutPicker) {
        PickerDialog(
            title = "Control layout",
            options = Store.layouts.map { it.name },
            selected = layoutName,
            onPick = { picked ->
                val l = Store.layouts.firstOrNull { it.name == picked } ?: return@PickerDialog
                Store.selectedLayoutId = l.id
                layoutId = l.id
                controls = l.controls
                selectedId = null
            },
            onDismiss = { showLayoutPicker = false }
        )
    }

    if (showAdd) {
        PickerDialog(
            title = "Add control",
            options = ControlAction.entries.map { "${it.label} (${it.key})" },
            selected = "",
            onPick = { picked ->
                val action = ControlAction.entries.firstOrNull { picked.startsWith(it.label) } ?: return@PickerDialog
                controls = controls + Control.of(action, 0.40f, 0.40f)
            },
            onDismiss = { showAdd = false }
        )
    }

    if (showRename) AlertDialog(
        onDismissRequest = { showRename = false },
        title = { Text("Rename layout") },
        text = {
            OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("Layout name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = {
                val name = renameText.trim()
                if (name.isNotEmpty()) {
                    Store.layouts = Store.layouts.map { if (it.id == layoutId) it.copy(name = name) else it }
                    showRename = false
                    onChanged()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
    )
}
