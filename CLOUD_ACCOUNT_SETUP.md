# Cloud Account setup (one-time, ~5 minutes)

The app's **Cloud Account** feature (Settings → Cloud Account) provides:

- Email/password sign-up, sign-in, and password reset
- Sessions that survive app restarts
- **Cloud library sync** — books, highlights, bookmarks and reading streaks are backed up
  to the signed-in user's private cloud document and can be restored on any device with
  the same account.

It uses **Firebase Authentication + Cloud Firestore** (Google's free Spark tier — no
credit card, no server to run). No Firebase SDK keys are stored in the Keys tab; the
single credential is a `google-services.json` file placed in the repo, which is how all
Android Firebase apps authenticate.

## 1. Create the Firebase project

1. Go to <https://console.firebase.google.com> and click **Add project** (Spark plan).
2. Give it any name (e.g. `a-hex-streak`), disable Analytics if you don't want it.

## 2. Register the Android app

1. In the project, click the **Android** icon to add an app.
2. Use **exactly** this package name:

   ```
   com.aistudio.ahexstreak.rxmpb
   ```

3. Nickname is optional. Skip SHA-1 (not needed for email/password auth).
4. Click **Register app**, then **Download google-services.json**.

## 3. Add the config file

Place the downloaded file at:

```
app/google-services.json
```

That's the only credential the app needs. The google-services plugin is already applied in
`app/build.gradle.kts`, and it is configured with `missingGoogleServicesStrategy = WARN` so the
app keeps building before you add this file. Until it is present, the account screen shows a
"One-time setup needed" notice and every action fails with a clear message instead of crashing.
After adding the file, sync Gradle and rebuild.

## 4. Enable Email/Password auth

1. Firebase Console → **Build → Authentication → Get started**.
2. In **Sign-in method**, enable **Email/Password** (leave "Email link" off).

## 5. Create the Firestore database + security rules

1. Firebase Console → **Build → Firestore Database → Create database**.
2. Start in **production mode** (locked by default — the rules below unlock it correctly).
3. Pick a region near your users.
4. Open the **Rules** tab and paste:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Each signed-in user can read/write ONLY their own library backup document.
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

5. Click **Publish**.

These rules enforce the security model in `AccountManager`: a user can never read or
write anyone's library but their own.

## 6. Build & test

1. Sync Gradle in Android Studio and run the app.
2. Settings → **Cloud Account** → **Sign In / Create Account**.
3. Create an account, then tap **Back Up Now**.
4. On a second device (or after reinstalling), sign in with the same account and tap
   **Restore Cloud** — your library, highlights and streak merge back in.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Toast: "Cloud sync was denied by security rules" | Step 5 rules not published, or wrong database region. |
| "No cloud library yet" on Restore | The account has never uploaded — press **Back Up Now** on the device that has the data first. |
| Sign-in says "network error" | Emulator without internet, or Firebase region blocked. |
| Build error `File google-services.json is missing` | The `googleServices { missingGoogleServicesStrategy = WARN }` block was removed from `app/build.gradle.kts` — restore it, or add the file from step 3. |
