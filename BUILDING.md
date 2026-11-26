# Building LocalTube

Native Android app (Kotlin + Jetpack Compose). See `AGENTS.md` for the full
stack and conventions.

## Open and build

1. Open the project root in **Android Studio** (Koala or newer). On first open,
   Android Studio generates the Gradle wrapper (`gradlew`, `gradle/wrapper/*.jar`)
   and syncs.
2. Build a debug APK: `./gradlew assembleDebug` (or Run ▶ onto a device/emulator).

> **Wrapper jar:** the binary `gradle/wrapper/gradle-wrapper.jar` is **not yet
> committed** (it was generated outside this environment). Android Studio adds it
> on first sync, or run `gradle wrapper --gradle-version 8.11.1` once and commit
> the result. CI provisions Gradle directly via `gradle/actions/setup-gradle`
> until the jar is committed.

## Stack

- Kotlin, Jetpack Compose + Material 3
- Media3 (ExoPlayer) for playback
- Room for metadata/history, DataStore for settings + the gate PIN
- Storage Access Framework for scoped folder access (no broad media permission)
- Manual DI container (`di/AppContainer.kt`) for the scaffold — Hilt is a planned
  follow-up (see `.ai/DECISIONS.md`).

## Status (Phase 1 MVP scaffold)

Implemented: SAF folder picking + scan, Room model, shuffled home feed, movies as
posters, show-folders as channels, Media3 player with up-next, parent PIN gate,
parent settings, main-folders mapping, daily screen-time limit + bedtime, and a
GitHub-Releases update checker.

**Not yet verified by a compile** — this was authored without an Android SDK
available. Expect a first compile pass to surface minor fixes (dependency
versions in `gradle/libs.versions.toml`, Media3 opt-ins). Run unit tests and
lint before relying on a build.

## Release & self-update

- `.github/workflows/release.yml` builds a **signed** release APK on every push to
  `main` using the **self-hosted runner** (`[self-hosted, linux, android]`) and
  publishes it to GitHub Releases, tagged `v1.0.<run_number>`.
- Required repo secrets (names only; set via `gh secret set <NAME> --repo rickevans86/LocalTube`):
  `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
  `RELEASE_KEY_PASSWORD`.
- The in-app updater (`update/UpdateChecker.kt`) reads the latest release and
  compares version codes; downloading + `PackageInstaller` handoff
  (`update/ApkInstaller.kt`) is wired but the download step is a follow-up.
