# Firebase setup

Destiny now uses:

- `Firebase Authentication` for email/password and Google sign-in
- `Cloud Firestore` for habits, revisions, and user profiles

## 1. Create the project

1. Open Firebase Console.
2. Create a project.
3. Add an Android app with package name `com.vishruthdev.destiny`.
4. Add your debug SHA-1 with `./gradlew signingReport`.

## 2. Enable Authentication

In `Authentication -> Sign-in method`:

- Enable `Email/Password`
- Enable `Google`

## 3. Create Firestore

1. Open `Firestore Database`.
2. Create the database in production mode.
3. Publish the rules from [`firestore.rules`](/Users/vishruthdev/AndroidStudioProjects/destiny/firestore.rules).

## 4. Add local config

Put these values in `local.properties`:

```properties
firebase.apiKey=YOUR_WEB_API_KEY
firebase.appId=YOUR_ANDROID_APP_ID
firebase.projectId=YOUR_PROJECT_ID
firebase.storageBucket=YOUR_STORAGE_BUCKET
firebase.gcmSenderId=YOUR_SENDER_ID
firebase.webClientId=YOUR_WEB_CLIENT_ID
```

## 5. Data model

Firestore collections:

- `users/{uid}`
- `users/{uid}/habits/{habitId}`
- `users/{uid}/revisionTopics/{topicId}`

Each user only reads and writes their own documents.

## 6. What changed in the app

- Login is now `email + password`, not username login.
- Username is stored as the profile display name.
- Google sign-in uses Credential Manager and exchanges the returned ID token with Firebase Auth.
- Habits and revisions are stored in Firestore under the signed-in user.
