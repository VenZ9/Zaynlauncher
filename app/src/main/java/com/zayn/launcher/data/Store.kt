package com.zayn.launcher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * All local persistence for the launcher. Three independent JSON blobs
 * (profiles / layouts / settings) plus a few selection keys, kept in
 * SharedPreferences so there is no database and no extra dependency.
 */
object Store {

    private const val FILE = "zayn_launcher"
    private const val K_PROFILES = "profiles"
    private const val K_LAYOUTS = "layouts"
    private const val K_SETTINGS = "settings"
    private const val K_SELECTED_PROFILE = "selected_profile"
    private const val K_SELECTED_LAYOUT = "selected_layout"
    private const val K_VERSION = "selected_version"
    private const val K_SEEDED = "seeded"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(K_SEEDED, false)) seed()
    }

    /** First run: give the user one profile and the two built-in control layouts. */
    private fun seed() {
        profiles = listOf(Profile.create("Player"))
        layouts = listOf(Layout.default(), Layout.pvp())
        prefs.edit()
            .putString(K_SELECTED_PROFILE, profiles.first().id)
            .putString(K_SELECTED_LAYOUT, "default")
            .putBoolean(K_SEEDED, true)
            .apply()
    }

    // ---------- profiles ----------

    var profiles: List<Profile>
        get() = readArray(K_PROFILES) { Profile.fromJson(it) }
        set(v) = writeArray(K_PROFILES) { v.forEach { p -> put(p.toJson()) } }

    var selectedProfileId: String
        get() = prefs.getString(K_SELECTED_PROFILE, "") ?: ""
        set(v) = prefs.edit().putString(K_SELECTED_PROFILE, v).apply()

    val activeProfile: Profile?
        get() = profiles.firstOrNull { it.id == selectedProfileId } ?: profiles.firstOrNull()

    // ---------- layouts ----------

    var layouts: List<Layout>
        get() = readArray(K_LAYOUTS) { Layout.fromJson(it) }
        set(v) = writeArray(K_LAYOUTS) { v.forEach { l -> put(l.toJson()) } }

    var selectedLayoutId: String
        get() = prefs.getString(K_SELECTED_LAYOUT, "") ?: ""
        set(v) = prefs.edit().putString(K_SELECTED_LAYOUT, v).apply()

    val activeLayout: Layout
        get() = layouts.firstOrNull { it.id == selectedLayoutId } ?: layouts.firstOrNull() ?: Layout.default()

    // ---------- settings ----------

    var settings: Settings
        get() = runCatching { Settings.fromJson(JSONObject(prefs.getString(K_SETTINGS, "{}") ?: "{}")) }
            .getOrDefault(Settings())
        set(v) = prefs.edit().putString(K_SETTINGS, v.toJson().toString()).apply()

    // ---------- version ----------

    var selectedVersion: String
        get() = prefs.getString(K_VERSION, Versions.LATEST_RELEASE) ?: Versions.LATEST_RELEASE
        set(v) = prefs.edit().putString(K_VERSION, v).apply()

    // ---------- reset ----------

    fun resetAll() {
        prefs.edit().clear().apply()
        seed()
    }

    fun resetLayouts() {
        layouts = listOf(Layout.default(), Layout.pvp())
        selectedLayoutId = "default"
    }

    // ---------- json helpers ----------

    private fun <T> readArray(key: String, parse: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun writeArray(key: String, build: JSONArray.() -> Unit) {
        val arr = JSONArray().apply(build)
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
