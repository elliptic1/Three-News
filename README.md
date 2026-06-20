# Three-News

A native Android news-display app. It shows the top three headlines from a
chosen [newsapi.org](https://newsapi.org) source, both in-app (a full-screen
3-pane layout) and via a home-screen widget. News is fetched in the background
through an Android `SyncAdapter` + `ContentProvider`, refreshed on an alarm.

## Modules

- **`mobile/`** – the app UI (`MainNewsActivity`, `NewsStoryFragment`,
  `SettingsFragment`, `MyAppWidget`).
- **`mysyncadapter/`** – background data layer (`MySyncAdapter`, `MyService`,
  `MyContentProvider`, `NewsAlarmManager`, `Authenticator`).

## Requirements

- JDK 17+
- Android SDK with platform/build-tools for **API 34**
- Android Gradle Plugin 8.5.2 (downloaded automatically), Gradle 8.7 (via the
  bundled wrapper)

## Setup

Add your newsapi.org API key to
`mysyncadapter/src/main/res/values/apikeys.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="newsapikey" translatable="false">PUT_YOUR_KEY_HERE</string>
</resources>
```

## Build

```sh
./gradlew :mobile:assembleRelease
```

The signed APK is written to `mobile/build/outputs/apk/release/`. The release
signing config and `keystore.jks` are committed for convenience (demo project).

## Notes

This project was modernized from its original 2016 toolchain (Gradle 2.14.1 /
AGP 2.2.1 / `jcenter` / the `android.support` libraries) to a current build:
Gradle 8.7, AGP 8.5.2, AndroidX, and the `google()` + `mavenCentral()`
repositories. The Firebase Analytics/Crash and Hugo logging integrations were
removed since they were non-essential and required dead dependencies/config.
