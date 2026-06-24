# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A native Android app (Java) that displays the top three headlines from a chosen
[Google News RSS](https://news.google.com/) source — both full-screen in-app (a 3-pane landscape layout)
and as a home-screen widget. Data is fetched in the background via an Android
`SyncAdapter` + `ContentProvider`, triggered on an alarm. Modernized in 2024 from the original
2016 toolchain (Gradle 2.14.1 / AGP 2.2.1 / jcenter / `android.support`) to Gradle 8.7 / AGP 8.5.2 /
AndroidX. Firebase Analytics/Crash and Hugo logging were removed during that migration.

## Build & test

```sh
./gradlew :mobile:assembleRelease      # signed release APK → mobile/build/outputs/apk/release/
./gradlew :mobile:assembleDebug        # debug build
./gradlew test                         # JVM unit tests (both modules)
./gradlew connectedAndroidTest         # instrumented tests (needs device/emulator)
./gradlew :mobile:testReleaseUnitTest  # single module's unit tests
```

Requires JDK 17, Android SDK for **API 34**. Gradle/AGP download via the bundled wrapper.

The release signing config and `keystore.jks` are committed intentionally (demo project) — keystore
password `threenewspass`, key alias `keyalias`. There is no separate debug-vs-release signing split.

## News source

Headlines come from **Google News RSS** — free, no API key, no signup, no quota. The feed URLs live in
`mysyncadapter/.../res/values/strings.xml`: `apiurl` is the per-source search feed
(`.../rss/search?q=site:%1$s+when:2d`) and `rss_top_url` is the unified top-stories feed. The selectable
sources are domains (e.g. `cnn.com`) in `newssources.xml`, plus the sentinel value `top` for the
top-stories feed; both arrays are index-aligned with `newssourcesnames.xml`.

The response is RSS XML, parsed with Android's built-in `XmlPullParser` (no extra dependency). Google
News RSS items have **no image**, so the `IMG` column is left null and story tiles show the placeholder.
Titles arrive as "Headline - Publisher"; the trailing publisher suffix is stripped on parse.

_History:_ the app originally used newsapi.org with a committed key in `apikeys.xml` (now deleted).
That endpoint (v1) was shut down and its free tier was dev-only — hence the move to RSS.

## Architecture

Two Gradle modules, with `:mobile` depending on `:mysyncadapter`:

- **`:mysyncadapter`** (`com.tbse.threenews.mysyncadapter`) — the headless data layer (an Android
  library). Owns fetching, storage, and the sync framework. No UI.
- **`:mobile`** (`com.tbse.threenews`, applicationId `com.tbse.nano.threenews`) — UI + widget.
  Reads data only through the ContentProvider; it never fetches news directly.

### Data flow (the part that needs multiple files to grasp)

1. **`NewsAlarmManager`** (BroadcastReceiver) fires on the repeating alarm and calls
   `ContentResolver.requestSync(...)` for the sync account against authority
   `com.tbse.threenews.provider`. The alarm is scheduled from `MainNewsActivity`.
2. The sync account is a dummy `AccountManager` account (`Authenticator` / `AuthenticatorService`)
   that exists only to drive the SyncAdapter framework — no real authentication.
3. **`MyService`** is the bound SyncAdapter service; it hands the framework a **`MySyncAdapter`**.
4. **`MySyncAdapter.onPerformSync`** reads the chosen source from SharedPreferences, does a Volley
   HTTP request to the Google News RSS feed for that source, parses the RSS XML with `XmlPullParser`,
   and writes rows into the ContentProvider via `CONTENT_URI`. Column constants
   (`_ID`, `IMG`, `SOURCE`, `HEADLINE`, `LINK`, `DATE`) are the contract — defined in
   `MyContentProvider` and imported statically by everyone else.
5. **`MyContentProvider`** persists into a SQLite DB (`newsdb`, table `news`, `DBVERSION`).
6. **`MainNewsActivity`** observes the provider with a `CursorLoader` + `ContentObserver` and
   renders the three stories; **`MyAppWidget`** reads the same provider for the widget.
7. **`MyTransform`** is a Picasso `Transformation` used to fit headline images to the pane size.

Images are loaded with **Picasso**, HTTP via **Volley**, dates via **Joda-Time**. The available
news sources are defined as parallel arrays in `res/values/newssources.xml` (source ids) and
`newssourcesnames.xml` (display names); `MySyncAdapter` zips them into a `sourceTo_name` map and
`SettingsFragment` surfaces them as a preference list.

### Things to watch

- **`android.nonTransitiveRClass=false`** in `gradle.properties` is deliberate: `:mobile` references
  string resources declared in `:mysyncadapter` via the merged R class. Don't flip it to `true`
  without qualifying those resource references.
- Column-name and authority/account-type constants are duplicated as static finals and imported
  statically across modules — changing one means updating the static imports in `MySyncAdapter`,
  `MainNewsActivity`, and `MyAppWidget`.
- The activity is locked to `landscape` and uses a fullscreen theme; the 3-pane layout assumes it.
