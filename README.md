# Task Manager App

A modern, offline-first Android task manager built with **Kotlin**, **Jetpack Compose**, and **Room**.

## Features

- Create, edit, delete, and complete tasks
- Priority levels (Low / Medium / High)
- Due dates
- Search and filter (All / Active / Completed)
- Material 3 design with dark mode support
- Local Room database (no internet required)
- Clean architecture (UI → ViewModel → Repository → Room)

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+
- Android SDK 34+
- Min SDK 26 (Android 8.0)

## Build the APK yourself

1. Clone this repository:
   ```bash
   git clone https://github.com/TTFabianstenq/task-manager-app.git
   cd task-manager-app
   ```

2. Open the project in Android Studio.

3. Let Gradle sync and download dependencies.

4. Build a release APK:
   - Menu → Build → Generate Signed Bundle / APK
   - Or from terminal:
     ```bash
     ./gradlew assembleRelease
     ```
   - Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

5. (Recommended) Sign the APK for installation on a real device.

## GitHub Actions

A workflow is included (`.github/workflows/build-apk.yml`) that builds a debug APK on every push to `main` and uploads it as an artifact. You can download it from the Actions tab.

For signed release APKs, configure signing secrets in the repository settings.

## Download Site

A simple download page is available via Vercel (see the website folder or the deployed site).

## License

MIT
