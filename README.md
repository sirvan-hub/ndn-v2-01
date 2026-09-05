<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# NDN v2.01 — Parcel/Logistics Management Platform

Kotlin + Jetpack Compose, Room (offline-first) + Supabase (cloud sync), MVVM, multi-role RBAC
(CUSTOMER, COURIER, HUB_MANAGER, ADMIN/SYSTEM_ADMIN).

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (Otter or newer), JDK 17.

1. Open Android Studio → **Open** → select this project's root directory.
2. Let Android Studio sync Gradle (this repo ships a real, working Gradle wrapper —
   `./gradlew`/`gradlew.bat` — pinned to Gradle 9.2.1, which AGP 9.1.1 requires).
3. Run `./gradlew assembleDebug` (or use the Run button) to build the debug APK.
   The output lands at `app/build/outputs/apk/debug/app-debug.apk`.
4. On first launch, sign in with the seeded System Admin account: username `Reza`,
   password `Admin@123`. **Change this password immediately** via
   Settings → Change Password — it is a public bootstrap credential, not a secret.

You do **not** need to create or configure a `debug.keystore` yourself — one is committed
at the project root (well-known `androiddebugkey`/`android` debug credentials, used only for
local/CI debug signing; it has no relation to the real Play Store release key).

## Building a release APK/AAB

The `release` build type reads signing credentials from environment variables
(`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) rather than hardcoded values —
set these locally, or as GitHub Secrets in CI, before running `./gradlew assembleRelease`.

## CI/CD: automatic APK builds via GitHub Actions

`.github/workflows/build-debug-apk.yml` builds a debug APK on every push/PR to `main`
(and on manual trigger) and uploads it as a run artifact — no local Android Studio
install needed to get an installable APK:

1. Push this project to a GitHub repository.
2. Go to the repo's **Actions** tab → the "Build Debug APK" workflow runs automatically.
3. Open the finished run → **Artifacts** → download `ndn-debug-apk-<run number>`.

If you'd like a signed **release** build wired into CI too (using GitHub Secrets for the
keystore), that's a small addition to the same workflow — ask and it can be added.

