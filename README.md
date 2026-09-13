# ZaynLauncher

A lightweight Android launcher for **Minecraft Java Edition**, in the spirit of Mojo Launcher / Zalith Launcher.
This repository contains the **app shell only** — profiles, controls, settings and the launcher UI.
The Minecraft Java / LWJGL runtime is intentionally *not* included; see "Runtime integration" below.

## Status: one small MVP milestone

Implemented:

- **Home** — active profile, Minecraft version picker, RAM picker, renderer placeholder, big Launch button.
- **Offline profiles** — unlimited local profiles (create / select / rename / delete), locally generated UUID.
- **Custom controls** — multiple layouts (Default, PvP, Custom), visual drag & resize editor, per-control
  opacity and enable toggle, save / reset / delete, stored as JSON.
- **Settings** — RAM, renderer placeholder, game directory placeholder, default layout, theme, reset.
- **Local storage** — SharedPreferences + JSON. No database, no network, no accounts.

Deliberately **not** implemented yet (later milestones): Minecraft Java runtime, LWJGL, Fabric loader,
version downloading, Microsoft authentication, renderer implementation, actual input injection.

## Build

Requirements: JDK 17, Android SDK (compileSdk 34), Gradle 8.7.

```bash
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

> Build verification: `./gradlew clean assembleDebug` completes with **BUILD SUCCESSFUL** and produces
> `app/build/outputs/apk/debug/app-debug.apk` (7.6 MB, `com.zayn.launcher`, minSdk 26 / targetSdk 34).
>
> If your build machine has ~2 GB of RAM, the defaults in `gradle.properties` are already tuned for it:
> a 1 GB Gradle heap, one worker, and `kotlin.compiler.execution.strategy=in-process`. Without these the
> daemon gets OOM-killed during dexing and Gradle reports
> *"build daemon disappeared unexpectedly"*. Raise `-Xmx` if you have plenty of RAM.

## Runtime integration (design note)

The app is deliberately split so a runtime can be dropped in without redesign:

- `data/Models.kt` holds `Profile`, `Control`, `Layout`, `Settings` and `Versions`.
- `data/Store.kt` is the only persistence surface. A runtime module only needs to read from it.
- `ui/HomeScreen.kt` contains the single Launch entry point. Today it reports
  *"Minecraft runtime not connected yet"*; the future runtime hook goes here.
- Controls are stored as fractions (0..1) of the play area, so any renderer surface can map them to
  pixels at its own resolution. `Control.action.key` is the placeholder binding an input layer will consume.

## Notes

- Offline UUIDs are derived as `UUID.nameUUIDFromBytes("OfflinePlayer:<name>")`, matching the convention
  Minecraft servers use for offline-mode players.
- No Microsoft authentication, no online account system, no analytics, no ads.
