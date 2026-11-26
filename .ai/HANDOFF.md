# LocalTube — Handoff

_Holds only the most recent handoff. Replace its contents each session; the
history lives in AI_ACTIVITY_LOG.md._

**Agent:** Claude

**Completed:** Phase 1 MVP on `main`, building and releasing. CI (self-hosted
runner HV01) compiles, signs and publishes — first release **v1.0.5**
(`app-release.apk`) is live on GitHub Releases. Screens: Home, Channel, Media3
Player, Parent gate, Settings, Main folders, Screen time; plus SAF scan, Room,
PIN gate, screen-time, and the GitHub-Releases update checker.

**In progress:** Nothing blocking. Hardening + tests next.

**Branch/PR:** `main`. `AGENTS.md` still in open PR #1.

**Tests:** Not added yet. **Lint:** Not run yet. **Build:** Green on CI.

**Things to do next:**
- **Install on a device and smoke-test** the real flows: add a folder (SAF), scan,
  shuffle feed, channel, Media3 playback, PIN gate, screen-time lock.
- **Commit the Gradle wrapper jar** (Android Studio generates it on first open),
  then switch `release.yml` back to `./gradlew`.
- Add unit tests + lint to the Definition of Done; wire them into CI.
- Merge AGENTS.md PR #1; ratify the stack in DECISIONS.md.

**Signing / CI (working):** RELEASE_* secrets set; keystore generated 2026-11-26;
runner HV01 (`self-hosted, linux, github-actions-linux`); SDK provisioned via
`android-actions/setup-android` (`packages: ''`).

**Feature backlog:** multiple profiles, favourites, continue-watching, bedtime UI,
subtitles, kiosk screen-pinning, the updater download + PackageInstaller step.

**Shared records updated:** Yes (PROJECT_STATE, AI_ACTIVITY_LOG, DECISIONS, this
file).

**Remote synchronization:** Confirmed (committed to a fresh clone of main).
