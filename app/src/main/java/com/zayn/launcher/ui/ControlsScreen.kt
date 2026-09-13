package com.zayn.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zayn.launcher.data.Control
import com.zayn.launcher.data.ControlAction
import com.zayn.launcher.data.Layout
import com.zayn.launcher.data.Store

/** Smallest / largest control size, as a fraction of the canvas. */
private const val MIN_SIZE = 0.06f
private const val MAX_SIZE = 0.5f

/** Press movement past which the gesture counts as a drag rather than a tap. */
private val TAP_SLOP = 8.dp

// ---------- geometry helpers (positions/sizes are fractions of the canvas) ----------

/** Keeps x >= 0 and x + w <= 1. */
private fun clampX(x: Float, w: Float): Float = x.coerceIn(0f, (1f - w).coerceAtLeast(0f))

/** Keeps y >= 0 and y + h <= 1. */
private fun clampY(y: Float, h: Float): Float = y.coerceIn(0f, (1f - h).coerceAtLeast(0f))

private fun clampW(w: Float): Float = w.coerceIn(MIN_SIZE, MAX_SIZE)

private fun clampH(h: Float): Float = h.coerceIn(MIN_SIZE, MAX_SIZE)

/** Clamps size first, then position, so the whole control stays inside the canvas. */
private fun Control.snapped(w: Float = this.w, h: Float = this.h): Control {
    val cw = clampW(w)
    val ch = clampH(h)
    return copy(w = cw, h = ch, x = clampX(x, cw), y = clampY(y, ch))
}

/**
 * Deterministic free spot for a newly added control, so additions cascade instead of
 * all landing on (0.40, 0.40) directly on top of each other.
 */
private fun spawnSpot(index: Int): Pair<Float, Float> {
    val step = 0.06f
    val slot = index % 8
    return (0.32f + (slot % 4) * step) to (0.32f + (slot / 4) * step)
}

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

    val density = LocalDensity.current
    val tapSlop = with(density) { TAP_SLOP.toPx() }
    val handlePx = with(density) { 16.dp.toPx() }

    val layoutName = Store.layouts.firstOrNull { it.id == layoutId }?.name ?: "Custom"
    val selected = controls.firstOrNull { it.id == selectedId }

    fun save() {
        Store.layouts = Store.layouts.map {
            if (it.id == layoutId) it.copy(controls = controls.map { c -> c.snapped() }) else it
        }
        onChanged()
    }

    fun replaceControl(updated: Control) {
        controls = controls.map { if (it.id == updated.id) updated else it }
    }

    // The gesture coroutine outlives a single composition, so it must never act on the
    // Control / canvas size captured when it started — those are stale the instant the
    // first drag moves anything. These handles always resolve to the current values.
    val currentBox by rememberUpdatedState(boxPx)
    val currentControls by rememberUpdatedState(controls)

    // Applies one gesture step computed from *fresh* state: the current control plus the
    // current canvas size. Movement therefore accumulates across the whole gesture and
    // tracks the finger 1:1 instead of being recomputed from a frozen origin.
    val applyGesture by rememberUpdatedState<(String, Boolean, Offset) -> Unit>({ id, isResize, delta ->
        val box = currentBox
        val c = currentControls.firstOrNull { it.id == id }
        if (c != null && box.width > 0 && box.height > 0) {
            val dx = delta.x / box.width
            val dy = delta.y / box.height
            replaceControl(
                if (isResize) c.snapped(w = c.w + dx, h = c.h + dy)
                else c.copy(x = clampX(c.x + dx, c.w), y = clampY(c.y + dy, c.h))
            )
        }
    })

    // One handler for both move and resize. Deliberately NOT remembered under the control
    // id alone: the keys are the control id plus the measured canvas, so the handler is
    // rebuilt whenever the canvas changes and reads live state on every event.
    fun gestureModifier(id: String, isResize: Boolean, onTap: () -> Unit): Modifier =
        Modifier.pointerInput(id, isResize, boxPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var travelled = Offset.Zero
                var dragged = false
                drag(down.id) { change ->
                    if (!change.isConsumed) {
                        val step = change.positionChange()
                        travelled += step
                        if (!dragged && travelled.getDistance() > tapSlop) dragged = true
                        if (dragged) {
                            applyGesture(id, isResize, step)
                            change.consume()
                        }
                    }
                }
                // A press that never moved is a tap, so tap-to-select still works.
                if (!dragged) onTap()
            }
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
                    key(c.id) {
                        // Re-clamped here as well, so a layout saved by an older build can
                        // never render outside the canvas.
                        val rx = clampX(c.x, c.w)
                        val ry = clampY(c.y, c.h)
                        val rw = clampW(c.w)
                        val rh = clampH(c.h)

                        // Fractions multiply the canvas size in PIXELS; those pixel values
                        // must not be handed to a .dp modifier, which would re-scale them by
                        // the density. Convert explicitly so touch and render units agree.
                        val offsetX = with(density) { (boxPx.width * rx).toDp() }
                        val offsetY = with(density) { (boxPx.height * ry).toDp() }
                        val sizeW = with(density) { (boxPx.width * rw).toDp() }
                        val sizeH = with(density) { (boxPx.height * rh).toDp() }
                        val controlPxW = boxPx.width * rw
                        val controlPxH = boxPx.height * rh

                        Box(
                            Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(sizeW, sizeH)
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
                                .then(gestureModifier(c.id, false) { selectedId = c.id })
                        ) {
                            Text(
                                c.action.label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            // Resize handle — drawn only when it actually fits inside the
                            // control, so it can't spill past the canvas edge.
                            if (controlPxW >= handlePx && controlPxH >= handlePx) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                                        .then(gestureModifier(c.id, true) { selectedId = c.id })
                                )
                            }
                        }
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
                onClick = {
                    // Reset the layout currently being edited — never a different one.
                    // The built-in geometry is re-applied, but each control keeps its
                    // existing identity (and opacity/enabled), so a reset restores
                    // positions instead of silently re-identifying the controls.
                    val pristine = if (layoutId == "pvp") Layout.pvp() else Layout.default()
                    val pool = controls.groupBy { it.action }
                    val used = HashMap<ControlAction, Int>()
                    controls = pristine.controls.map { fresh ->
                        val seen = used.getOrDefault(fresh.action, 0)
                        used[fresh.action] = seen + 1
                        val existing = pool[fresh.action]?.getOrNull(seen)
                        fresh.copy(
                            id = existing?.id ?: fresh.id,
                            opacity = existing?.opacity ?: fresh.opacity,
                            enabled = existing?.enabled ?: fresh.enabled
                        )
                    }
                    selectedId = null
                },
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
                val (sx, sy) = spawnSpot(controls.size)
                val added = Control.of(action, sx, sy).snapped()
                controls = controls + added
                selectedId = added.id
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
