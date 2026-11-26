# LocalTube — Project State

**Repository:** rickevans86/LocalTube
**Updated:** 2026-11-26
**Phase:** 1 — MVP on main; CI builds + releases a signed APK (v1.0.5)

## What LocalTube is
An offline, kid-safe Android video player that plays videos already stored on the
device, presented in a YouTube Kids–style interface: big thumbnails, no search,
no ads, no outbound links. Content is parent-curated, gated behind a parent PIN,
with per-profile screen-time limits and a kiosk lock. No accounts and no network,
except an optional self-updater.

## Stack (proposed — see DECISIONS.md; awaiting Rick's approval)
Kotlin · Jetpack Compose + Material 3 · libVLC (playback) · Room · SAF scoped
folder access · Hilt · Gradle (Kotlin DSL) + version catalog. minSdk 26,
targetSdk 35.

## Current status
- Phase 1 MVP scaffold committed to `main` (Kotlin/Compose). See `BUILDING.md`.
- Implemented: SAF folder pick + scan, Room model, shuffled home feed, movies as
  posters, show-folders as channels, Media3 player + up-next, parent PIN gate,
  parent settings, main-folders mapping, screen-time limit + bedtime, GitHub-
  Releases update checker.
- `release.yml` present, targeting **self-hosted runner HV01**
  (`[self-hosted, linux, github-actions-linux]`), provisions the Android SDK,
  signs with the RELEASE_* secrets, and publishes to GitHub Releases.
- **Builds green on CI** — first signed release **v1.0.5** published to GitHub
  Releases (`app-release.apk`). Build fix applied: added the Material Components
  dependency for the `Theme.Material3.*` XML parent. Unit tests/lint still TODO.
- Gradle wrapper jar not committed yet (Android Studio generates it on open; CI
  provisions Gradle directly meanwhile).
- `AGENTS.md` is in open PR #1 (stack still marked proposed).

## Architecture (summary)
MVVM with a repository layer, four layers top-down, each depending only on the
one below:
1. **UI** — Compose screens + ViewModels (profile picker, kid home, player,
   parent settings)
2. **Use cases** — scan content, screen-time, parent gate, play session
3. **Repositories** — content, profile, settings, history
4. **Platform & data** — SAF files, Room, Media3, Keystore/LockTask,
   GitHub-Releases updater

## Immediate next steps
See HANDOFF.md and TASK_BOARD.md.

## Source of truth
GitHub is authoritative (Issues for tasks, commits/PRs for changes). These
`.ai/` files are summaries; if they ever conflict with GitHub history, GitHub
wins — investigate and correct the file.
