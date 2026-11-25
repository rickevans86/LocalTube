# LocalTube — Project State

**Repository:** rickevans86/LocalTube
**Updated:** 2026-11-26
**Phase:** 0 — Coordination & pipeline setup (pre-feature)

## What LocalTube is
An offline, kid-safe Android video player that plays videos already stored on the
device, presented in a YouTube Kids–style interface: big thumbnails, no search,
no ads, no outbound links. Content is parent-curated, gated behind a parent PIN,
with per-profile screen-time limits and a kiosk lock. No accounts and no network,
except an optional self-updater.

## Stack (proposed — see DECISIONS.md; awaiting Rick's approval)
Kotlin · Jetpack Compose + Material 3 · Media3 (ExoPlayer) · Room · SAF scoped
folder access · Hilt · Gradle (Kotlin DSL) + version catalog. minSdk 26,
targetSdk 35.

## Current status
- Full architecture + feature spec drafted (LocalTube spec doc).
- No application code in the repo yet.
- Coordination system (AGENTS.md + `.ai/` files) being established — this file
  is part of that setup.
- Release pipeline (`release.yml`) and built-in self-updater not yet created.

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
