package com.zayn.launcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A local, offline Minecraft profile. No Microsoft / online account is involved.
 * The UUID is derived the same way Minecraft derives "OfflinePlayer" UUIDs, so the
 * profile presents a stable identity to a server without any authentication.
 */
data class Profile(
    val id: String,
    val username: String,
    val uuid: String
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("username", username)
        .put("uuid", uuid)

    companion object {
        fun create(username: String): Profile = Profile(
            id = UUID.randomUUID().toString(),
            username = username,
            uuid = offlineUuid(username)
        )

        fun offlineUuid(username: String): String =
            UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString()

        fun fromJson(o: JSONObject) = Profile(
            id = o.getString("id"),
            username = o.getString("username"),
            uuid = o.optString("uuid", offlineUuid(o.getString("username")))
        )
    }
}

/** Every action a touch control can be bound to. Keys are placeholders for the future runtime. */
enum class ControlAction(val label: String, val key: String) {
    FORWARD("Forward", "W"),
    BACK("Back", "S"),
    LEFT("Left", "A"),
    RIGHT("Right", "D"),
    JUMP("Jump", "Space"),
    SNEAK("Sneak", "Shift"),
    SPRINT("Sprint", "Ctrl"),
    ATTACK("Attack", "LMB"),
    USE("Use Item", "RMB"),
    INVENTORY("Inventory", "E"),
    CHAT("Chat", "T"),
    DROP("Drop", "Q"),
    HOTBAR_1("Hotbar 1", "1"),
    HOTBAR_2("Hotbar 2", "2"),
    HOTBAR_3("Hotbar 3", "3"),
    HOTBAR_4("Hotbar 4", "4"),
    HOTBAR_5("Hotbar 5", "5"),
    HOTBAR_6("Hotbar 6", "6"),
    HOTBAR_7("Hotbar 7", "7"),
    HOTBAR_8("Hotbar 8", "8"),
    HOTBAR_9("Hotbar 9", "9"),
    MOUSE_MOVE("Mouse", "MOUSE")
}

/**
 * One on-screen control. Position and size are stored as fractions (0..1) of the
 * play area so a layout looks identical on any screen size.
 */
data class Control(
    val id: String,
    val action: ControlAction,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val opacity: Float = 1f,
    val enabled: Boolean = true
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("action", action.name)
        .put("x", x.toDouble())
        .put("y", y.toDouble())
        .put("w", w.toDouble())
        .put("h", h.toDouble())
        .put("opacity", opacity.toDouble())
        .put("enabled", enabled)

    companion object {
        fun of(action: ControlAction, x: Float, y: Float, w: Float = 0.11f, h: Float = 0.11f) =
            Control(UUID.randomUUID().toString(), action, x, y, w, h)

        fun fromJson(o: JSONObject) = Control(
            id = o.getString("id"),
            action = runCatching { ControlAction.valueOf(o.getString("action")) }
                .getOrDefault(ControlAction.FORWARD),
            x = o.optDouble("x", 0.1).toFloat(),
            y = o.optDouble("y", 0.1).toFloat(),
            w = o.optDouble("w", 0.11).toFloat(),
            h = o.optDouble("h", 0.11).toFloat(),
            opacity = o.optDouble("opacity", 1.0).toFloat(),
            enabled = o.optBoolean("enabled", true)
        )
    }
}

/** A named set of controls, stored locally as JSON. */
data class Layout(
    val id: String,
    val name: String,
    val controls: List<Control>
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("controls", JSONArray().apply { controls.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(o: JSONObject): Layout {
            val arr = o.optJSONArray("controls") ?: JSONArray()
            return Layout(
                id = o.getString("id"),
                name = o.getString("name"),
                controls = (0 until arr.length()).map { Control.fromJson(arr.getJSONObject(it)) }
            )
        }

        /** Built-in layout: movement on the left, actions on the right. */
        fun default() = Layout(
            id = "default",
            name = "Default",
            controls = listOf(
                Control.of(ControlAction.FORWARD, 0.10f, 0.16f),
                Control.of(ControlAction.LEFT, 0.01f, 0.32f),
                Control.of(ControlAction.BACK, 0.10f, 0.48f),
                Control.of(ControlAction.RIGHT, 0.19f, 0.32f),
                Control.of(ControlAction.JUMP, 0.78f, 0.20f),
                Control.of(ControlAction.SNEAK, 0.66f, 0.32f),
                Control.of(ControlAction.SPRINT, 0.90f, 0.32f, 0.09f, 0.09f),
                Control.of(ControlAction.ATTACK, 0.78f, 0.60f),
                Control.of(ControlAction.USE, 0.88f, 0.72f, 0.09f, 0.09f),
                Control.of(ControlAction.INVENTORY, 0.66f, 0.08f, 0.08f, 0.08f),
                Control.of(ControlAction.CHAT, 0.55f, 0.08f, 0.08f, 0.08f),
                Control.of(ControlAction.HOTBAR_1, 0.06f, 0.80f, 0.07f, 0.07f),
                Control.of(ControlAction.HOTBAR_2, 0.14f, 0.80f, 0.07f, 0.07f),
                Control.of(ControlAction.HOTBAR_3, 0.22f, 0.80f, 0.07f, 0.07f),
                Control.of(ControlAction.HOTBAR_4, 0.30f, 0.80f, 0.07f, 0.07f),
                Control.of(ControlAction.HOTBAR_5, 0.38f, 0.80f, 0.07f, 0.07f)
            )
        )

        /** Built-in layout: tighter right-hand cluster aimed at combat. */
        fun pvp() = Layout(
            id = "pvp",
            name = "PvP",
            controls = listOf(
                Control.of(ControlAction.FORWARD, 0.12f, 0.14f),
                Control.of(ControlAction.LEFT, 0.02f, 0.30f),
                Control.of(ControlAction.BACK, 0.12f, 0.46f),
                Control.of(ControlAction.RIGHT, 0.22f, 0.30f),
                Control.of(ControlAction.JUMP, 0.80f, 0.46f, 0.13f, 0.13f),
                Control.of(ControlAction.ATTACK, 0.62f, 0.52f, 0.14f, 0.14f),
                Control.of(ControlAction.USE, 0.78f, 0.66f, 0.12f, 0.12f),
                Control.of(ControlAction.SPRINT, 0.90f, 0.34f, 0.09f, 0.09f),
                Control.of(ControlAction.SNEAK, 0.66f, 0.70f, 0.10f, 0.10f),
                Control.of(ControlAction.INVENTORY, 0.66f, 0.06f, 0.08f, 0.08f),
                Control.of(ControlAction.HOTBAR_1, 0.04f, 0.82f, 0.07f, 0.07f),
                Control.of(ControlAction.HOTBAR_2, 0.12f, 0.82f, 0.07f, 0.07f),
                Control.of(ControlAction.HOTBAR_3, 0.20f, 0.82f, 0.07f, 0.07f)
            )
        )
    }
}

/** Launcher-wide settings. */
data class Settings(
    val ramMb: Int = 2048,
    val renderer: String = "Not connected",
    val gameDir: String = "/storage/emulated/0/ZaynLauncher",
    val defaultLayoutId: String = "default",
    val darkTheme: Boolean = true
) {
    fun toJson() = JSONObject()
        .put("ramMb", ramMb)
        .put("renderer", renderer)
        .put("gameDir", gameDir)
        .put("defaultLayoutId", defaultLayoutId)
        .put("darkTheme", darkTheme)

    companion object {
        val RAM_OPTIONS = listOf(1024, 2048, 3072, 4096, 6144, 8192)
        val RENDERERS = listOf("Not connected", "GL4ES (placeholder)", "VirGL (placeholder)", "Zink (placeholder)")

        fun fromJson(o: JSONObject) = Settings(
            ramMb = o.optInt("ramMb", 2048),
            renderer = o.optString("renderer", "Not connected"),
            gameDir = o.optString("gameDir", "/storage/emulated/0/ZaynLauncher"),
            defaultLayoutId = o.optString("defaultLayoutId", "default"),
            darkTheme = o.optBoolean("darkTheme", true)
        )
    }
}

/** Minecraft versions offered by the picker. Version *downloading* is out of scope for this milestone. */
object Versions {
    const val LATEST_RELEASE = "1.21.1"
    val ALL = listOf(
        "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9"
    )
}
