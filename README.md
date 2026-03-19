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

## How to Use

### Getting Started

1. **Sign in** with your Google account on the login screen
2. You land on the **Today** tab — your daily dashboard showing today's habits and due revisions
3. Use the bottom navigation to switch between **Today**, **Habits**, **Revisions**, and **Settings**

### Habit Tracking

**Creating a habit:**
- Go to the **Habits** tab and tap **Add new Habit**
- Enter a name (e.g., "Gym", "Read 30 pages")
- Choose when to start: Today, Tomorrow, or pick a custom date
- Set the daily time you plan to do it

**Daily workflow:**
- Open the **Today** tab to see all habits due for the day
- **Tap once** on a habit to mark it **In Progress** (orange dot) — an "In progress" label appears below the name
- **Tap again** to mark it **Completed** (green checkmark)
- **Tap a third time** to reset it back to **Not Started**
- When all habits are completed, a celebration message appears with an undo option (10-second countdown)

**Tracking progress:**
- Each habit card in the Habits tab shows your **current streak** and **30-day completion rate**
- Missed days are highlighted in red — if you skip a day, Destiny tracks it and shows how many days you've missed

### Spaced Revision (1-2-4-7)

The 1-2-4-7 method is a spaced repetition technique: after learning something, you revise it on **Day 1**, **Day 2**, **Day 4**, and **Day 7** for optimal retention.

**Creating a revision topic:**
- Go to the **Revisions** tab and tap **Add topic**
- Enter the topic name (e.g., "Chapter 5 — Thermodynamics")
- Choose the start date and the time you want to be reminded

**Revision workflow:**
- Each topic shows 4 day nodes (Day 1, 2, 4, 7) with visual states:
  - **Locked** (grey lock) — previous day not yet completed
  - **Active** (blue outline) — ready to revise today
  - **In Progress** (orange filled) — revision started
  - **Completed** (green checkmark) — done
  - **Overdue** (red) — you missed the scheduled day
- Tap **Start Day X** to begin a revision (moves to In Progress)
- Tap **Mark Day X done** to complete it (moves to Completed, unlocks the next day)
- If a revision is overdue, you can **Catch up** or **Reset from today** to restart the schedule

### Reminders & Alarms

Every habit and revision gets two reminders by default:
- **10 minutes before** — a notification appears as a heads-up
- **2 minutes before** — a device alarm rings with sound and vibration

**Toggling alarms:**
- In the **Habits** or **Revisions** tab, **tap any card** to flip it
- The back of the card shows an alarm toggle switch
- Turn it off to disable reminders for that specific habit/revision
- Tap the card again to flip back

### Tips for Efficient Use

- **Morning routine:** Open the Today tab each morning to see what's due — habits on top, revisions below
- **Use In Progress:** Mark habits as "In Progress" when you start them, then complete when done — this helps you track what you're actively working on
- **Don't ignore overdue revisions:** Either catch up or reset — leaving them overdue blocks future days from unlocking
- **Set realistic times:** Schedule habits at times you'll actually do them — the 10-min + 2-min alarm combo ensures you won't forget
- **Review the Habits tab weekly:** Check your streaks and completion rates to see which habits need more attention

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

This project uses Firebase for authentication and data sync. See the setup guides:

- [Firebase Setup](docs/FIREBASE_SETUP.md)
- [Google Sign-In Setup](docs/GOOGLE_SIGNIN_SETUP.md)

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
