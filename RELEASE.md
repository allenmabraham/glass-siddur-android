# Releasing Siddur for Android

## One-time setup

1. **Upload key.** Create it once and keep it safe (losing it means a new app listing):
   ```sh
   keytool -genkeypair -v -keystore ~/keys/siddur-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **`keystore.properties`** in the repo root (gitignored):
   ```
   storeFile=/Users/you/keys/siddur-upload.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
   Without this file, `bundleRelease` signs with the debug key, which Play rejects.
3. **Play Console** (play.google.com/console, one-time $25 registration): create the app "Siddur", default language English, free, and turn on Play App Signing (it is on by default for new apps).

## Each release

1. Bump `versionCode` (must increase every upload) and `versionName` in `app/build.gradle.kts`.
2. Run the checks and build the bundle:
   ```sh
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testDebugUnitTest :app:lintRelease :app:bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab` (about 7.5 MB; the texts are stored uncompressed and delivered through the bundle).
3. Smoke-test the minified build on a device or emulator (`./gradlew :app:assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk`): open a prayer, pinch to zoom, fold and unfold, toggle a bookmark, open Settings.
4. In Play Console → Production → Create new release → upload the `.aab`, write the release notes, roll out.

## Store listing (ready to paste)

- **Name:** Siddur
- **Short description (80):** A beautiful siddur: Hebrew with synced English, four nusachim, works offline.
- **Full description:** see `docs/store-listing.md`.
- **Category:** Books & Reference. **Content rating:** Everyone (IARC questionnaire: no violence, no user content, no ads).
- **Privacy policy URL:** host `docs/privacy-policy.md` (for example on GitHub Pages) and paste the URL. The listing needs one because the app can request location.
- **Data safety form:** Location (approximate) — optional, used for app functionality, not shared, not collected off-device. No other data.
- **Screenshots:** `docs/screenshots` (phone: `cover-*.png` at 884×2208; 7-inch tablet / foldable: `open-*.png`, `book-*.png` at 1768×2208). Play wants at least two phone screenshots and a 1024×500 feature graphic.
- **App icon:** 512×512 PNG, export from `app/src/main/res/drawable/ic_launcher_foreground.xml` over the `#4D78B8` background (Android Studio → Image Asset, or `docs/icon-512.png`).

## Foldable testing

`README.md` describes the 7.6-inch fold-in AVD and the console commands (`adb emu fold | unfold | posture 2 | rotate`).
