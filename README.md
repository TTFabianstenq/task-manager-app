# Task Manager App

A modern, offline-first Android task manager built with **Kotlin**, **Jetpack Compose**, and **Room**.

## Features

- Create, edit, delete tasks
- **End Task** (explicit complete) and **Reopen** actions
- Checkbox toggle for quick complete/reopen
- Priority levels (Low / Medium / High)
- Due dates with date picker + overdue highlighting
- Optional categories
- Search (title, description, category)
- Filters: All / Active / Completed
- Live stats in the top bar (active · done)
- Confirm before deleting a task or clearing completed
- Material 3 design with dark mode / dynamic color
- Fully offline (Room database)

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+
- Android SDK 35
- Min SDK 26 (Android 8.0)

## Build the APK

### Via GitHub Actions (easiest)

1. Go to [Actions](https://github.com/TTFabianstenq/task-manager-app/actions)
2. Open the latest **Build APK** run
3. Download the **TaskManager-debug** artifact
4. Unzip and install the APK on your phone

### Locally

```bash
git clone https://github.com/TTFabianstenq/task-manager-app.git
cd task-manager-app
# Open in Android Studio once (generates wrapper if needed), or rely on CI
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

For a signed release APK use **Build → Generate Signed Bundle / APK** in Android Studio.

## Download site

https://task-manager-download.vercel.app

## License

MIT
