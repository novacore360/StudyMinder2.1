# StudyMinder 📚

A lightweight Kotlin Android app to manage your academic schedule — quizzes, exams, laboratories, assignments and more. No sign-up, no sign-in, fully local storage.

## ✨ Features

- **No account needed** — just enter your name once on first launch.
- **Dashboard** — view upcoming schedules (next 48h) and all schedules, sorted by approaching deadline. Edit, delete, or mark as done.
- **Subjects** — add subjects with subject ID, professor name, and default room.
- **Schedule** — create quizzes/exams/labs/etc, pick a subject (auto-fills professor/room, editable), set custom reminder time.
- **Smart notifications** — reminders ring + vibrate, repeat every 5 minutes until acknowledged or marked done. If ignored past the deadline, the item is automatically marked **Missed**.
- **Mocha Green theme** — warm, calming, beautiful UI with rounded cards and soft colors.
- **100% local storage** (SharedPreferences + Gson) — no backend, no internet required.
- **Background service + AlarmManager + BroadcastReceiver** — reminders work even when the app is closed.

## 🏗️ Project Structure

```
app/src/main/java/com/studyminder/
├── data/
│   ├── model/         (Subject, Schedule, ScheduleType, ScheduleStatus)
│   └── repository/    (StudyRepository - local storage)
├── service/
│   ├── AlarmReceiver.kt          (schedules/handles alarms, notifications)
│   ├── NotificationActionReceiver.kt (Acknowledge / Mark Done actions)
│   └── NotificationService.kt    (foreground service)
├── ui/
│   ├── onboarding/    (Splash, Onboarding - ask name)
│   ├── dashboard/     (Dashboard fragment)
│   ├── subjects/      (Subjects fragment + Add/Edit dialog)
│   ├── schedule/      (Schedule fragment + Add/Edit dialog)
│   └── MainActivity.kt
└── adapter/           (RecyclerView adapters)
```

## 🚀 Build with Codemagic

This repo includes a `codemagic.yaml`. Steps:

1. Push this project to a Git repository (GitHub/GitLab/Bitbucket).
2. Connect the repo in Codemagic.
3. The included workflow `studyminder-android` will:
   - Generate the Gradle wrapper jar if missing.
   - Build a debug APK (`assembleDebug`).
   - Build a release APK (`assembleRelease`).
4. Download the APK artifact from the build results.

### Local build
```bash
gradle wrapper --gradle-version 8.4   # generates gradle-wrapper.jar (one time)
./gradlew assembleDebug
```

## 🔔 Notification Behavior

1. When you add a schedule, pick "remind before" (5 min – 24 hrs).
2. At the reminder time, you get a notification with **Acknowledge** and **Mark Done** buttons, vibration, and sound.
3. If not acknowledged, it repeats **every 5 minutes**.
4. If the deadline passes without acknowledgement or "done", the item is automatically marked **Missed** ⚠️.

## 🎨 Theme

Mocha Green palette — deep forest green (`#3E6343`), warm cream backgrounds (`#FDFAF5`), and soft brown accents (`#6B4C3B`) for a cozy, focused study vibe.

## 📋 Requirements

- minSdk 26 (Android 8.0+)
- targetSdk 34
- Kotlin 1.9
