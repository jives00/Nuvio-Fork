# Fork Changes — Direct Scrobble to Trakt App

This fork adds direct scrobbling to a self-hosted Trakt clone app
(`https://trakt.berek.xyz`) without requiring a Trakt.tv account.

All changes are tagged `// [FORK]` in-code for easy identification.

---

## What Was Changed

### New Files (no upstream conflict possible)

| File | Purpose |
|---|---|
| `app/src/main/java/com/nuvio/tv/data/remote/api/DirectScrobbleApi.kt` | Retrofit interface — `POST start` and `POST stop` endpoints with `X-Api-Key` header |
| `app/src/main/java/com/nuvio/tv/data/repository/DirectScrobbleService.kt` | Service that calls the API; no-ops silently if `SCROBBLE_API_URL` is blank |

These are entirely new files. They will never conflict on merge.

---

### Modified Files

#### `app/build.gradle.kts`

Three separate additions:

1. **Lint block** (after `compileSdk`): disables fatal lint errors for local sideload builds
   ```kotlin
   // [FORK] disable lint fatal errors for local sideload builds
   lint {
       checkReleaseBuilds = false
       abortOnError = false
   }
   ```

2. **BuildConfig fields** (in `defaultConfig`, after `PREMIUMIZE_CLIENT_ID`):
   ```kotlin
   // [FORK] Direct scrobble endpoint config
   buildConfigField("String", "SCROBBLE_API_URL", "\"${localProperties.getProperty("SCROBBLE_API_URL", "")}\"")
   buildConfigField("String", "SCROBBLE_API_KEY", "\"${localProperties.getProperty("SCROBBLE_API_KEY", "")}\"")
   ```

3. **Debug signing** (in `debug` buildType, first line): changed from release keystore to debug keystore for local sideload builds
   ```kotlin
   signingConfig = signingConfigs.getByName("debug") // [FORK] use debug keystore for local sideload builds
   ```

#### `app/src/main/java/com/nuvio/tv/core/di/NetworkModule.kt`

Two `@Provides` functions appended at the end of the module (after `SeriesGraphApi`):
```kotlin
// [FORK] Direct scrobble Retrofit instance
@Provides @Singleton @Named("direct")
fun provideDirectScrobbleRetrofit(...): Retrofit { ... }

// [FORK]
@Provides @Singleton
fun provideDirectScrobbleApi(@Named("direct") retrofit: Retrofit): DirectScrobbleApi { ... }
```

#### `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeController.kt`

One line added to the constructor parameter list:
```kotlin
internal val directScrobbleService: DirectScrobbleService, // [FORK]
```

#### `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerViewModel.kt`

Two lines: one constructor parameter, one pass-through to `PlayerRuntimeController`:
```kotlin
private val directScrobbleService: DirectScrobbleService, // [FORK]
// ...
directScrobbleService = directScrobbleService, // [FORK]
```

#### `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerPlaybackEvents.kt`

Three call sites — one `start` and two `stop` — each a single line appended after the existing Trakt scrobble call:
```kotlin
directScrobbleService.start(item, progressPercent) // [FORK]
directScrobbleService.stop(item, percent)          // [FORK]
directScrobbleService.stop(item, progressPercent)  // [FORK]
```

#### `app/src/main/java/com/nuvio/tv/core/player/ExternalPlaybackTracker.kt`

One constructor parameter added, plus the Trakt auth guard was restructured so `directScrobbleService` calls sit **outside** the `isAuthenticated` check (the guard still wraps the Trakt calls):
```kotlin
private val directScrobbleService: DirectScrobbleService, // [FORK]
// ...
// [FORK] Direct scrobble — no Trakt auth required
directScrobbleService.start(scrobbleItem, progressPercent = 0f)
directScrobbleService.stop(scrobbleItem, progressPercent = progressPercent)
```

---

## Local Config (not committed)

**`local.properties`** — add these two lines:
```
SCROBBLE_API_URL=https://trakt.berek.xyz/api/scrobble/nuvio/
SCROBBLE_API_KEY=<value from server .env>
```

**`local.dev.properties`** — add these two lines (used by debug builds):
```
SUPABASE_URL=https://dpyhjjcoabcglfmgecug.supabase.co
SUPABASE_ANON_KEY=<extracted from official APK>
```

---

## Merging Upstream Changes

When pulling upstream changes from `NuvioMedia/NuvioTV`:

1. **New files** — `DirectScrobbleApi.kt` and `DirectScrobbleService.kt` will never conflict; keep them as-is.

2. **`build.gradle.kts`** — If upstream touches `defaultConfig` or `buildTypes.debug`, re-apply the three `[FORK]` blocks after merging. They are self-contained additions, not edits to existing lines.

3. **`NetworkModule.kt`** — If upstream adds new `@Provides` functions at the bottom, just ensure the two `[FORK]` functions remain present anywhere in the module.

4. **`PlayerRuntimeController.kt` / `PlayerViewModel.kt`** — If upstream adds or reorders constructor parameters, re-add `directScrobbleService` as a parameter and pass-through. Position doesn't matter.

5. **`PlayerRuntimeControllerPlaybackEvents.kt`** — If upstream changes `emitScrobbleStart`, `emitScrobbleStop`, or `emitPauseScrobbleStop`, ensure each function still calls `directScrobbleService.start/stop` after the Trakt call. These are the three functions containing `// [FORK]`.

6. **`ExternalPlaybackTracker.kt`** — If upstream changes the scrobble block, ensure `directScrobbleService` calls remain **outside** the Trakt `isAuthenticated` guard.

---

## Server-Side Counterpart

The Trakt app (`https://github.com/jives00/trakt`) handles these scrobbles at:
- `POST /api/scrobble/nuvio/start` — updates now-playing
- `POST /api/scrobble/nuvio/stop` — records watch history at 80% (movie) / 70% (episode)

Auth: `X-Api-Key` header matching `SCROBBLE_API_KEY` in server `.env`.
