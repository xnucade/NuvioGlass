# Nuvio Glass

A fork of [NuvioTV](https://github.com/NuvioMedia/NuvioTV) that adds a frosted-glass home layout
and a Calendar tab, targeting the **Onn 4K** (base box: Android 12, Pro: Android 14).

Prebuilt APKs are on the [Releases page](https://github.com/xnucade/NuvioGlass/releases). The Onn
4K is **arm64**, so take `app-full-arm64-v8a-release.apk` unless you know you need otherwise.

Upstream is kept as the `upstream` remote on the `dev` branch. Everything here is additive — no
upstream file is rewritten — so rebasing on `dev` stays cheap.

## Why a fork and not a rewrite

NuvioTV already ships the parts that take months to build: the Stremio addon client, Media3 and
libmpv playback, Trakt and Simkl sync, TMDB metadata, and account sync. The glass work is a
presentation-layer change, so it is built as a fourth `HomeLayout` alongside Classic, Grid and
Modern rather than as a new app.

Upstream already depends on [Haze](https://github.com/chrisbanes/haze) (1.7.2) and already blurs
its sidebar panel, so the blur plumbing was in place before this fork started.

## Toolchain

Installed on this machine for this project:

| Tool | Version | Location |
| --- | --- | --- |
| JDK | 21.0.12 | `/opt/homebrew/opt/openjdk@21` |
| Android SDK | platform 36, build-tools 36.0.0 | `/opt/homebrew/share/android-commandlinetools` |
| Platform tools | 37.0.1 (adb) | same, under `platform-tools/` |
| Android Studio | latest | `/Applications/Android Studio.app` |

The NDK is **not** needed. The only native build in the project is the Dolby Vision bridge, gated
behind `DOVI_NATIVE_ENABLED`, which is off by default. The prebuilt `.so` files under
`app/src/main/jniLibs` ship as-is.

## Building

Use `./gb` rather than `./gradlew`. It pins the JDK and SDK above and, importantly, redirects all
Gradle output to `~/Library/Caches/NuvioGlassBuild`.

```bash
./gb assembleFullDebug
```

That redirect matters because this repo lives under `~/Desktop`, which is iCloud-synced. Left
alone, Gradle would push several GB of build artifacts into iCloud and the file provider would
re-stamp extended attributes on build output. The redirect is opt-in via `-PnuvioBuildRoot`; with
the flag absent the build behaves exactly like upstream.

The APK lands in:

```
~/Library/Caches/NuvioGlassBuild/app/build/outputs/apk/full/debug/
```

## What this fork adds

- **Glass home layout** — a fourth `HomeLayout` beside Classic, Grid and Modern. Frosted top
  navigation and clock over a full-bleed hero, no sidebar. Pick it in Settings under Layout, or on
  first run.
- **Calendar tab** — an airing schedule derived from the series in your library, one week back and
  four weeks ahead. As complete as your metadata add-ons are; a series whose add-on omits air dates
  cannot be scheduled.
- **Glass design system** — `ui/components/glass/`, reused for the player stats HUD.

## Installing on the Onn 4K

Enable Developer options on the box (Settings → System → About → tap Build seven times), then
turn on USB/network debugging. With the Onn on the same network:

```bash
adb connect <onn-ip>:5555
adb install -r ~/Library/Caches/NuvioGlassBuild/app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk
```

The Onn 4K is arm64. The build also produces armeabi-v7a, x86, x86_64 and a universal APK.

## Keys

`local.properties` is gitignored and starts blank. Fill in what you need:

- `TMDB_API_KEY` — artwork and metadata. Without it most posters and backdrops stay empty.
- `TRAKT_CLIENT_ID` / `TRAKT_CLIENT_SECRET` — Trakt sync.
- `NUVIO_SUPABASE_URL` / `NUVIO_SUPABASE_ANON_KEY` — account sign-in and sync: add-ons, watch
  history, playback progress. Blank runs the app signed out, which is what the published release
  APK ships as. Whoever builds the fork points it at a backend they control.

`nuviotv.jks` is a locally generated debug keystore, also gitignored. Upstream's build script signs
even debug builds, so the file has to exist for any build to succeed.

## The glass design system

Under `app/src/main/java/com/nuvio/tv/ui/components/glass/`:

- **`GlassSurface.kt`** — `Modifier.glassSurface()`, the one place the frosted treatment is
  defined. Blur, then tint, then edge. The edge is what sells it: a hairline that runs bright at
  the top to nearly invisible at the bottom, the way a bevel catches light from above.
- **`GlassNavPill.kt`** — top navigation. One glass surface holds every destination, so the bar
  costs a single blur pass instead of one per item.
- **`GlassClockPill.kt`** — clock and date. Ticks on the minute, not the second.
- **`GlassBadge.kt`** — hero metadata chips, with a `flat` mode that drops the blur.

Tokens live in `ui/theme/GlassTokens.kt`.

### Performance rules this follows

Taken from what the Onn can actually sustain:

1. **Blur only the chrome.** The nav pill, clock pill and badges blur. Poster cards never do.
2. **Blur at two-thirds scale.** `HazeInputScale.Fixed(0.66f)`, matching upstream's sidebar.
3. **Blur a backdrop that rarely changes.** The hero only crossfades when focus moves rows, so the
   blur is recomputed on that event rather than per frame.
4. **No blur during playback.** `LocalGlassBlurEnabled` turns it off over moving video, where a
   blur costs a render pass per frame and buys no legibility.
5. **Degrade below Android 12.** Live blur needs `RenderEffect`. On a Shield (Android 11) or a Fire
   TV stick the surfaces fall back to an opaque tint instead of dropping frames. Both Onn boxes
   clear the bar, so this is for other hardware, not yours.

## Licence

Upstream is GPLv3, so this fork is too. That only binds you if you distribute builds; personal use
carries no obligation.
