# LocalTube — AI Activity Log

Append-only. Add one dated entry at the end. Do not edit or reorder earlier
entries.

---

**2026-11-26 — Claude**
- Drafted the LocalTube architecture + feature specification.
- Prepared the initial `.ai/` coordination files: PROJECT_STATE, TASK_BOARD,
  AI_ACTIVITY_LOG, HANDOFF, DECISIONS, FILE_OWNERSHIP.
- Context: website mode, no direct repository access. These files have **not**
  been committed — they await a Phase 0 setup PR by Rick/Codex.
- Tests: Not run (no code).
- PR: None yet.

---

**2026-11-26 — Claude**
- Built the Phase 1 MVP app scaffold (Kotlin/Jetpack Compose): Gradle setup +
  version catalog, Room data layer, SAF `MediaScanner`, `SettingsStore`
  (DataStore + salted PIN), `ContentRepository`, manual DI container, Compose
  theme + navigation, and screens — Home (shuffled feed, movie posters, show
  channels), Channel, Media3 Player + up-next, Parent gate, Settings, Main
  folders, Screen time.
- Added `update/UpdateChecker.kt` + `ApkInstaller.kt` (GitHub Releases) and
  `.github/workflows/release.yml` targeting the self-hosted runner.
- Committed directly to `main` at Rick's explicit direction (overriding the
  "no app code to main by hand" rule).
- Tests: Not run. Lint: Not run. Build: **Not run locally** — verified via CI.
- Commit: see `main`. Follow-ups in HANDOFF.md.

---

**2026-11-26 — Claude**
- Stood up the release pipeline end to end on self-hosted runner HV01:
  aligned `runs-on` to its labels (`self-hosted, linux, github-actions-linux`),
  provisioned the Android SDK (`setup-android` with `packages: ''` to skip the
  removed `tools` package), switched signing to the `RELEASE_*` secrets.
- Fixed the one build error CI surfaced: added `com.google.android.material`
  for the `Theme.Material3.*` XML parent.
- Result: CI builds, signs and publishes. First release **v1.0.5**
  (`app-release.apk`) is live on GitHub Releases.
- Rick set the four `RELEASE_*` signing secrets; keystore generated 2026-11-26.
- Tests/lint: still to be added.
