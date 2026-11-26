# LocalTube — Handoff

_Holds only the most recent handoff. Replace its contents each session; the
history lives in AI_ACTIVITY_LOG.md._

**Agent:** Claude

**Completed:** Phase 1 MVP app scaffold committed to `main` — Gradle setup, Room
data layer, SAF scanner, settings/PIN store, repository, Compose theme + nav, and
all screens (Home, Channel, Media3 Player, Parent gate, Settings, Main folders,
Screen time), plus the GitHub-Releases update checker and `release.yml`
(self-hosted runner).

**In progress:** First compile pass.

**Branch/PR:** Committed to `main`. `AGENTS.md` still in open PR #1.

**Tests:** Not run. **Lint:** Not run. **Build:** Not run (no Android SDK in the
authoring environment).

**Blockers / things to verify first:**
- **Compile the project in Android Studio.** Expect minor fixes: dependency
  versions in `gradle/libs.versions.toml`, Media3 `@UnstableApi` opt-ins, and any
  Compose API drift. This is the top priority before anything else.
- **Commit the Gradle wrapper jar** (Android Studio generates it on first open),
  then switch `release.yml` back to `./gradlew`.
- Signing secrets set (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) — workflow + build.gradle use these
  names. Keystore generated 2026-11-26.
- Runner HV01 confirmed (labels `self-hosted, linux, github-actions-linux`);
  workflow provisions the Android SDK via `android-actions/setup-android`.
- AGENTS.md PR #1 still open; stack still marked proposed.

**Next action:**
1. Open in Android Studio, sync, `./gradlew assembleDebug`, fix compile errors.
2. Add the wrapper jar; run unit tests + lint.
3. Set signing secrets; let the self-hosted runner cut the first release.
4. Then iterate features (favourites, continue-watching, kiosk pinning, the
   updater download step) as Issues.

**Shared records updated:** Yes (PROJECT_STATE, AI_ACTIVITY_LOG, DECISIONS, this
file).

**Remote synchronization:** Confirmed (committed to a fresh clone of main).
