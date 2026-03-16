# Google Sign-In setup

Destiny now uses `Firebase Auth + Credential Manager` for Google sign-in.

## What you need

1. A Firebase project
2. Google sign-in enabled in Firebase Auth
3. An Android OAuth client for your app package and SHA-1
4. A Web OAuth client ID for ID token exchange with Firebase

## Steps

1. Open Firebase Console and create or select your project.
2. In `Authentication -> Sign-in method`, enable `Google`.
3. In `Project settings -> Your apps`, register the Android app:
   - Package name: `com.vishruthdev.destiny`
   - Add your debug SHA-1
4. In `Project settings -> General`, note these values:
   - `App ID`
   - `Project ID`
   - `Web API Key`
   - `Storage bucket` if shown
   - `Sender ID`
5. In `Project settings -> General -> Your apps -> SDK setup and configuration`, copy the `Web client ID`.
6. Add these values to `local.properties`:

```properties
firebase.apiKey=YOUR_WEB_API_KEY
firebase.appId=YOUR_ANDROID_APP_ID
firebase.projectId=YOUR_PROJECT_ID
firebase.storageBucket=YOUR_STORAGE_BUCKET
firebase.gcmSenderId=YOUR_SENDER_ID
firebase.webClientId=YOUR_WEB_CLIENT_ID
```

## Getting your SHA-1

Run:

```bash
./gradlew signingReport
```

Use the `SHA1` value from the `debug` variant while developing. Add your release SHA-1 before shipping production builds.

## Notes

- The app initializes Firebase from `local.properties`, so `google-services.json` is not required for this setup.
- If `firebase.webClientId` is missing, email/password auth still works but the Google button stays disabled.
