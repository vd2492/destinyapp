# Destiny

A daily habit tracker and spaced revision app for Android, built with Kotlin and Jetpack Compose.

Destiny combines habit tracking with a 1-2-4-7 spaced revision system to help you build lasting habits and retain what you study.

## Features

**Habit Tracking**
- Create daily habits with custom start dates and times
- Three-state completion: Not Started → In Progress → Completed
- Streak tracking and 30-day completion rate
- 30-day streak milestone dialog with restart, delete, or continue options
- Optional Strict Mode — when enabled, skipping a day restarts the habit from Day 1; when off (default), missed days keep your progress

**Spaced Revision (1-2-4-7)**
- Schedule revision topics with automatic day spacing (Day 1, 2, 4, 7)
- Smart progression — complete previous days before moving forward
- Completion dialog for fully finished topics with restart or delete options
- Optional Strict Mode — when enabled, skipping a revision day restarts the plan from Day 1; when off (default), missed days keep your progress

**Reminders**
- Notification 10 minutes before each habit/revision
- Device alarm 2 minutes before, with alarm sound and vibration
- Per-habit and per-revision alarm toggle that turns both reminders on or off (tap the card to flip and configure)
- Exact alarms are granted by the user (a one-time prompt and a card in Settings), so reminders arrive on time
- Alarms persist across device reboots

**Today Dashboard**
- Due Habits and Due Revisions cards, each with its own progress ring
- A ring only appears when something is due today, and becomes a green tick when that list is finished
- All-completed celebration state when every habit is done for the day, with a 10-second Undo all window

**Accounts & Cloud Sync**
- Sign up with just an email and password, or continue with Google
- Password reset by email
- Editable display name in Settings
- Account deletion with re-authentication, removing the account and all of its data
- Real-time Firestore sync across devices
- Strict per-user security rules

**Help & Personalisation**
- Written in-app **Tutorial** (Settings → Tutorial) with step-by-step guides, examples and tips
- One-time "New to Destiny?" prompt on first login that links to the tutorial
- Dark / light theme switch (sun and moon icon) on the Today tab and in Settings, remembered between launches

**UI**
- Material 3 design with dark and light theme
- Flip card animation for alarm settings
- Bottom navigation: Today, Habits, Revisions, Settings
- Version label in the Settings tab

## How to Use

### Getting Started

1. **Create an account** with an email and password, or tap **Sign in with Google**. Forgot your password? Enter your email on the Login tab and tap **Forgot password?**
2. A first-time prompt offers the **Tutorial**. Tap **Open tutorial**, or **Maybe later** (it is always under **Settings → Tutorial**)
3. You land on the **Today** tab — your daily dashboard showing today's habits and due revisions
4. Use the bottom navigation to switch between **Today**, **Habits**, **Revisions**, and **Settings**
5. The cards at the top of the **Today** tab update live as you complete habits or start/finish due revisions. Each has a progress ring while something is due today

### Habit Tracking

**Creating a habit:**
- Go to the **Habits** tab and tap **Add new Habit**
- Enter a name (e.g., "Gym", "Read 30 pages")
- Choose when to start: Today, Tomorrow, or pick a custom date
- Set the daily time you plan to do it

**Daily workflow:**
- Open the **Today** tab to see all habits due for the day
- The **Due Habits** and **Due Revisions** cards update instantly as you make progress
- **Tap once** on a habit to mark it **In Progress** (orange dot) — an "In progress" label appears below the name
- **Tap again** to mark it **Completed** (green checkmark)
- **Tap a third time** to reset it back to **Not Started**
- When **every** habit for today is completed, the list is replaced by a celebration banner with an **Undo all** option (10-second countdown)

**Strict Mode (auto-reset on miss):**
- Controlled by the **Strict Mode** toggle in the **Settings** tab (off by default)
- When **on**: if you miss a day (don't complete a habit), it automatically restarts from Day 1 the next day — your streak resets to 0 and completion history clears, enforcing an unbroken chain
- When **off**: missed days no longer reset anything; your habits and revision topics keep their start date and history, so an absence (e.g. not opening the app for a while) won't wipe your progress
- The same switch controls revision topics

**Deleting a habit:**
- On the **Habits** tab tap **Remove habit** (or long-press any card), tap the **✕** on the habit, then **Done**
- Deletion is immediate and permanent, with no confirmation or undo

**Tracking progress:**
- Each habit card in the Habits tab shows your **current streak**, your **completion rate** and a red tag such as "Missed yesterday" if you skipped a day
- When a habit reaches a **30-day streak**, a popup lets you **restart**, **delete**, or **continue** the streak
- If you dismiss that popup, the habit keeps going and you can reopen the same options from the flipped card via **Edit options**

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
- Tap **Start Day X** to begin a revision (moves to In Progress)
- Tap **Mark Day X done** to complete it (moves to Completed, unlocks the next day)
- After finishing the full **Day 1, 2, 4, 7** cycle, a popup lets you **restart** or **delete** the topic
- If you dismiss that popup, the topic stays completed and you can reopen the same options from the flipped card via **Edit options**
- With **Strict Mode** on, missing a revision day restarts the topic from **Day 1** on the current day. With it off (the default), your progress is kept
- To delete a topic: **Remove topic** (or long-press a card), tap the **✕**, then **Done**. This is immediate and permanent

### Reminders & Alarms

Every habit and revision gets two reminders by default:
- **10 minutes before** — a notification appears as a heads-up
- **2 minutes before** — a device alarm rings with sound and vibration

For the alarm to fire at the exact time, allow **Alarms & reminders** when prompted (or later from the card in **Settings**). Notifications need the notification permission on Android 13+.

**Toggling alarms:**
- In the **Habits** or **Revisions** tab, **tap any card** to flip it
- The back of the card shows the **Alarm (2 min before)** switch, which controls both reminders
- Completed 30-day habits and fully completed revision topics also show an **Edit options** button on the back
- Turn it off to disable both reminders for that specific habit/revision
- Tap the card again to flip back

### Settings

- **Account:** shows who is signed in, with **Edit name** and **Logout**
- **Tutorial:** opens the written guide to the app
- **Theme:** switch between dark and light (also available on the Today tab)
- **Strict Mode:** see above
- **Alarms & reminders:** a card appears if exact alarm access has not been granted
- **Delete account:** permanently removes your account and all data after you confirm with your password or Google
- The bottom-left corner shows the current app version

### Tips for Efficient Use

- **Morning routine:** Open the Today tab each morning to see what's due — habits on top, revisions below
- **Never break the chain:** A missed day breaks your streak. With Strict Mode on it also resets the habit to Day 1, so complete every day to keep it alive
- **Use In Progress:** Mark habits as "In Progress" when you start them, then complete when done — this helps you track what you're actively working on
- **Stay consistent with revisions:** Later days stay locked until the earlier ones are done. With Strict Mode on, a missed day restarts the plan from Day 1
- **Set realistic times:** Schedule habits at times you'll actually do them — the 10-min notification and 2-min alarm ensure you won't forget
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
| Notifications | FCM (Cloud Functions) + local AlarmManager |
| Build | Gradle 8.13, KSP |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Project Structure

```
com.vishruthdev.destiny
├── data/
│   ├── HabitRepository.kt          # Habits + revisions business logic
│   ├── AuthRepository.kt           # Auth, user profiles, display name, account deletion
│   └── SettingsRepository.kt       # Per-account preferences (Strict Mode)
├── viewmodel/
│   ├── HomeViewModel.kt            # Today screen state
│   ├── HabitsViewModel.kt          # Habits screen state
│   └── RevisionsViewModel.kt       # Revisions screen state
├── ui/
│   ├── HomeScreen.kt               # Daily overview
│   ├── HabitsScreen.kt             # Habit management + flip cards
│   ├── RevisionsScreen.kt          # Revision management + flip cards
│   ├── SettingsScreen.kt           # Settings, edit name, theme, delete account
│   ├── TutorialScreen.kt           # Written in-app guide
│   ├── TutorialPromptDialog.kt     # One-time first-login prompt
│   ├── ThemeToggle.kt              # Sun/moon theme switch
│   ├── ThemePreferenceStore.kt     # Remembers the chosen theme
│   ├── LoginScreen.kt              # Sign up, login, Google, password reset
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

functions/                          # Cloud Functions that send reminder pushes
docs/                               # Setup guides + public privacy and deletion pages
```

## Firebase Setup

This project uses Firebase for authentication and data sync. Sign-in with Email/Password and Google must both be enabled. Google sign-in on a Play Store build also needs the **Play app signing key** SHA-1 added to the Firebase Android app, alongside your upload key and debug key. See the setup guides:

- [Firebase Setup](docs/FIREBASE_SETUP.md)
- [Google Sign-In Setup](docs/GOOGLE_SIGNIN_SETUP.md)

## Firestore Schema

```
users/{uid}                          # uid, email, displayName, created/updated timestamps
├── settings/app                     # strictModeEnabled
├── habits/{habitId}
│   ├── name, startDateMillis, startHour, startMinute
│   ├── completionDates[], inProgressDates[]
│   └── alarmEnabled, thirtyDayDialogDismissed
├── revisionTopics/{topicId}
│   ├── name, startDateMillis, revisionHour, revisionMinute
│   ├── completedDays[], inProgressDays[]
│   └── alarmEnabled, completionDialogDismissed
└── notificationTokens/{installationId}
```

## Build & Run

```bash
git clone https://github.com/vd2492/destinyapp.git
cd destinyapp
# Add local.properties with Firebase credentials (see docs/FIREBASE_SETUP.md)
./gradlew assembleDebug
```

**Release build (Google Play):**

```bash
cp keystore.properties.example keystore.properties   # fill in your keystore details
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Bump `versionCode` in `app/build.gradle.kts` for every upload.

**Checks:**

```bash
./gradlew testDebugUnitTest lintDebug
```

## Permissions

- `INTERNET` — Cloud sync
- `POST_NOTIFICATIONS` — Reminder notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` — Exact device alarms (granted by the user)
- `RECEIVE_BOOT_COMPLETED` — Reschedule alarms after reboot
