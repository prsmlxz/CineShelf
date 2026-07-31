# CineShelf

A clean, minimal, iOS-inspired video library and player for Android. Point it at
folders you create for each show or movie, drop downloaded video files into
them from your device, and CineShelf organizes everything automatically —
including detecting `S03E01`-style episode filenames and sorting them into
season folders without ever renaming the original file.

## Features

- **Library shelf** — grid of your shows/movies with poster art pulled from
  the video itself, watch-progress indicator, pull-to-refresh
- **Auto season detection** — drop `Show.Name.S03E01.mkv` into a show's
  folder and CineShelf moves it into a `Season 3` subfolder automatically,
  displaying it as "Episode 1" while leaving the actual filename untouched
- **Movies & specials** — files with no season/episode pattern are listed
  as standalone items; a folder with a single video jumps straight to a
  movie-style hero screen
- **Long-press actions** — long-press any tile for an animated overlay with
  a watched toggle and a delete button; deleting shows an Undo snackbar
  before the file is actually removed from disk
- **Custom video player** — Media3 ExoPlayer wrapped in fully custom,
  minimal controls: tap to show/hide, double-tap left/right to seek ±10s,
  scrubber with time labels, playback speed menu, resume from last position,
  auto-mark-watched near the end, immersive fullscreen playback
- **All Files Access** — uses the device's `Movies/CineShelf/<Show>/` folder
  so files you download or move in from any app (browser, file manager,
  etc.) show up automatically on next refresh

## Project structure

This is a standard Gradle-based native Android project (Kotlin + Jetpack
Compose). There's no Android Studio project file lock-in — it'll open in
Android Studio directly, or build headlessly via Gradle/CI.

```
CineShelf/
├── app/
│   └── src/main/java/com/cineshelf/app/
│       ├── data/        # filesystem scanning, filename parsing, metadata store
│       ├── ui/           # library, detail, player, permission screens
│       └── navigation/    # nav graph
├── .github/workflows/build.yml   # builds a debug APK on every push
└── build.gradle.kts / settings.gradle.kts
```

## Getting your APK (no local Android setup required)

This repo includes a GitHub Actions workflow that compiles a debug APK
automatically. To use it:

1. **Create a new repository on GitHub** (public or private — your choice).
2. **Upload this project's contents to it.** Easiest way from a machine with
   git installed:
   ```bash
   cd CineShelf
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
   (No git installed? You can also drag-and-drop all the files/folders into
   the GitHub web UI's "Add file → Upload files" screen — just make sure the
   folder structure is preserved, including the hidden `.github` folder.)
3. **Go to the "Actions" tab** on your GitHub repo. A workflow run should
   start automatically (it triggers on every push to `main`). It takes a
   few minutes — it's downloading the Android SDK and all dependencies.
4. **Download the APK.** Once the run finishes (green check), click into
   it, scroll to "Artifacts", and download `CineShelf-debug-apk`. Unzip it
   to get `app-debug.apk`.
5. **Install it on your device.** Transfer the APK to your Android phone
   and open it (you'll need to allow "install unknown apps" for whichever
   app you use to open it — Files, Chrome, etc., since this isn't going
   through the Play Store).

If a workflow run doesn't start automatically, use the "Run workflow"
button under Actions → Build APK → Run workflow (this works because the
workflow also listens for manual `workflow_dispatch` triggers).

## First launch

On first open, CineShelf will ask for **All Files Access** — this is what
lets it create show folders under `Movies/CineShelf/` and see files you
download into them from other apps. Grant it in the settings screen that
opens, then come back to the app.

## Notes on this build

- This produces a **debug** APK (unsigned, fine for installing directly on
  your own device). If you ever want a signed release build for
  distributing more broadly, that needs a signing key set up separately.
- Minimum supported Android version is **Android 11 (API 30)**, which keeps
  the storage-permission code simple and consistent across devices.
- Recognized video extensions: mp4, mkv, avi, mov, webm, m4v, 3gp, ts, flv.
- Season/episode detection supports `S03E01` and `3x01` style filenames.
