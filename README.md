# Destiny

A daily habit tracker and spaced revision app for Android, built with Kotlin and Jetpack Compose.

Destiny combines habit tracking with a 1-2-4-7 spaced revision system to help you build lasting habits and retain what you study.

## Features

**Habit Tracking**
- Create daily habits with custom start dates and times
- Three-state completion: Not Started → In Progress → Completed
- Streak tracking and 30-day completion rate
- Missed day detection with visual indicators

**Spaced Revision (1-2-4-7)**
- Schedule revision topics with automatic day spacing (Day 1, 2, 4, 7)
- Smart progression — complete previous days before moving forward
- Overdue detection with catch-up or reset options

**Reminders**
- 10-minute notification before each habit/revision
- 2-minute device alarm with alarm sound and vibration
- Per-habit and per-revision alarm toggle (tap card to flip and configure)
- Alarms persist across device reboots

**Celebration**
- All-completed celebration state when every habit is done for the day
- Undo window with countdown timer

**Cloud Sync**
- Google Sign-In authentication
- Real-time Firestore sync across devices
- Strict per-user security rules

**UI**
- Material 3 design with dark and light theme
- Flip card animation for alarm settings
- Bottom navigation: Today, Habits, Revisions, Settings

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository |
| State | StateFlow / MutableStateFlow |
| Auth | Firebase Auth + Credential Manager |
| Database | Cloud Firestore (real-time) |
| Notifications | FCM + local AlarmManager |
| Build | Gradle 8.13, KSP |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Project Structure

```
com.vishruthdev.destiny
├── data/
│   ├── HabitRepository.kt          # Habits + revisions business logic
│   └── AuthRepository.kt           # Auth & user profiles
├── viewmodel/
│   ├── HomeViewModel.kt            # Today screen state
│   ├── HabitsViewModel.kt          # Habits screen state
│   └── RevisionsViewModel.kt       # Revisions screen state
├── ui/
│   ├── HomeScreen.kt               # Daily overview
│   ├── HabitsScreen.kt             # Habit management + flip cards
│   ├── RevisionsScreen.kt          # Revision management + flip cards
│   ├── SettingsScreen.kt           # Settings & logout
│   ├── LoginScreen.kt              # Authentication
│   └── theme/                      # Colors, typography, theme
├── reminder/
│   ├── ReminderScheduler.kt        # Dual-stage alarm scheduling
│   ├── ReminderAlarmReceiver.kt    # Alarm broadcast handler
│   ├── ReminderNotificationManager.kt  # Notification channels
│   └── ReminderBootReceiver.kt     # Reschedule on reboot
├── push/
│   ├── DestinyFirebaseMessagingService.kt
│   └── PushTokenSyncManager.kt
├── DestinyApplication.kt           # App initialization
└── MainActivity.kt                 # Entry point
```

## Firebase Setup

1. Create a Firebase project and register an Android app with package `com.vishruthdev.destiny`
2. Enable **Email/Password** and **Google** sign-in in Firebase Auth
3. Create a **Firestore Database** in production mode
4. Deploy security rules: `firebase deploy --only firestore:rules`
5. Add your SHA-1 fingerprint (`./gradlew signingReport`) to the Firebase console
6. Create a Web OAuth client for Google Sign-In
7. Add credentials to `local.properties`:

```properties
firebase.apiKey=YOUR_WEB_API_KEY
firebase.appId=YOUR_ANDROID_APP_ID
firebase.projectId=YOUR_PROJECT_ID
firebase.storageBucket=YOUR_STORAGE_BUCKET
firebase.gcmSenderId=YOUR_SENDER_ID
firebase.webClientId=YOUR_WEB_CLIENT_ID
```

See [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) and [docs/GOOGLE_SIGNIN_SETUP.md](docs/GOOGLE_SIGNIN_SETUP.md) for detailed guides.

## Firestore Schema

```
users/{uid}
├── habits/{habitId}
│   ├── name, startDateMillis, startHour, startMinute
│   ├── completionDates[], inProgressDates[]
│   └── alarmEnabled
├── revisionTopics/{topicId}
│   ├── name, startDateMillis, revisionHour, revisionMinute
│   ├── completedDays[], inProgressDays[]
│   └── alarmEnabled
└── notificationTokens/{installationId}
```

## Build & Run

```bash
git clone https://github.com/vishruthdev/destiny.git
cd destiny
# Add local.properties with Firebase credentials (see above)
./gradlew assembleDebug
```

## Permissions

- `INTERNET` — Cloud sync
- `POST_NOTIFICATIONS` — Reminder notifications (Android 13+)
- `USE_EXACT_ALARM` — Device alarms
- `RECEIVE_BOOT_COMPLETED` — Reschedule alarms after reboot
