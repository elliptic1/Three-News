# Three-News

A native Android news-display app. It shows the top three headlines from a
chosen source, both in-app (a full-screen 3-pane layout) and via a home-screen
widget. Headlines come from [Google News RSS](https://news.google.com/) — free,
no API key required. News is fetched in the background through an Android
`SyncAdapter` + `ContentProvider`, refreshed on an alarm.

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

None — headlines are fetched from public Google News RSS feeds, so there is no
API key to configure. Pick a source from the in-app settings; the default is CNN.

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

The news source was later migrated from newsapi.org (whose v1 endpoint was shut
down and whose free tier requires a key and is dev-only) to Google News RSS,
which is free and keyless. The fetch still uses Volley; the response is RSS XML
parsed with Android's built-in `XmlPullParser`. Google News RSS items carry no
image, so story tiles show a placeholder.
