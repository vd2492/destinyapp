# Google Sign-In setup

For **Sign in with Google** to work, you must create an OAuth 2.0 client in Google Cloud and register your app.

## Steps

1. **Google Cloud Console**  
   Go to [Google Cloud Console](https://console.cloud.google.com/) and create or select a project.

2. **OAuth consent screen**  
   In **APIs & Services → OAuth consent screen**, configure the consent screen (External or Internal, app name, support email).

3. **Get your app’s SHA-1**  
   - **Debug:**  
     `./gradlew signingReport`  
     or in Android Studio: **Gradle → app → Tasks → android → signingReport**  
   - Use the **SHA-1** under `Variant: debug`.

4. **Create Android OAuth client**  
   - **APIs & Services → Credentials → Create credentials → OAuth client ID**  
   - Application type: **Android**  
   - Name: e.g. `Destiny Android`  
   - Package name: `com.vishruthdev.destiny`  
   - SHA-1: paste the value from step 3  
   - Create

5. **Run the app**  
   Install and run the app. The **Sign in with Google** button will use this client. No code changes are required; the default `GoogleSignInOptions.DEFAULT_SIGN_IN` uses your app’s package and signing key.

For **release** builds, add a second OAuth client with your **release** keystore’s SHA-1 and the same package name.
