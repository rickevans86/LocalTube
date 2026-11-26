# LocalTube — Architectural Decisions

Append-only. Add one dated entry at the end. Do not edit or reorder earlier
entries.

---

**2026-11-26 — Adopt the 3-AI collaboration protocol** (AI: Claude)
LocalTube follows Rick's Universal 3-AI GitHub Development Protocol: ChatGPT for
architecture/planning, Codex for implementation, Claude for review. GitHub is
the single source of truth; Rick assigns tasks and merges to main.

**2026-11-26 — Proposed stack (AWAITING RICK'S APPROVAL — not yet ratified)**
(AI: Claude)
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Playback: Media3 (ExoPlayer) — local codecs, playlist auto-advance
- Persistence: Room (metadata/profiles/history); DataStore + Android Keystore
  for prefs and the parent-gate hash
- Content access: Storage Access Framework scoped tree URIs — avoids broad
  media permissions
- DI: Hilt
- Build: Gradle (Kotlin DSL) + version catalog; Gradle wrapper committed
- minSdk 26, targetSdk 35

Reasons: consistency with Rick's other Compose apps (FlipRSS, Mihon fork);
Media3 is the standard local-playback stack; SAF keeps a kid-safe app to the
minimum permissions. Once approved, confirm this entry and mirror the stack into
the AGENTS.md Project section. Do not switch toolkit, architecture or major
libraries later without a new Issue and Rick's approval.

**2026-11-26 — Sideloaded distribution + self-update** (AI: Claude, proposal)
LocalTube ships as a signed APK via GitHub Releases and self-updates (not Play
Store). The release is cut by CI from main; `versionCode`/`versionName` are
derived in CI, never hand-edited. Prefer a public repo so the updater needs no
token.

**2026-11-26 — Scaffold DI: manual container, not Hilt (yet)** (AI: Claude)
The MVP scaffold uses a hand-written `AppContainer` instead of Hilt, to keep the
first build free of annotation-processor setup while it is unverified. Hilt
remains the target per the stack above; migrating is a planned follow-up Issue.

**2026-11-26 — CI uses self-hosted runner; Gradle provisioned by CI** (AI: Claude)
`release.yml` runs on `[self-hosted, linux, android]` (confirm labels in
AGENTS.md). The Gradle wrapper jar is not committed yet (binary not generatable
in the authoring environment), so CI provisions Gradle via
`gradle/actions/setup-gradle`; commit the wrapper jar once generated locally and
switch CI back to `./gradlew`.

**2026-11-26 — Playback backend: libVLC, not Media3/ExoPlayer** (AI: Claude)
On-device testing showed SAF `content://` videos not playing under ExoPlayer
(codec/URI handling). Switched to libVLC (`org.videolan.android:libvlc-all`),
fed a file descriptor opened from the SAF URI — it handles far more
codecs/containers. Replaces Media3 in the player; the custom transport UI is
minimal for now (play/pause + back + up-next).

**2026-11-26 — Reverted playback to Media3 ExoPlayer** (AI: Claude)
libVLC caused a native (uncatchable) crash on opening a video on-device, even
after deferring attach/play to on-window. Switched back to Media3 ExoPlayer:
plays `content://` URIs directly, PlayerView gives built-in controls + seek bar,
and errors arrive as catchable `Player.Listener.onPlayerError` (shown on screen).
If a real codec gap appears, add FFmpeg/NextLib software decoders to ExoPlayer
rather than returning to libVLC.

**2026-11-26 — App committed to main at Rick's direction** (AI: Claude)
The Phase 1 scaffold was pushed straight to `main` on Rick's explicit
instruction, overriding the base prompt's "agents do not push app code to main
by hand". Recorded for history; the branch → PR → Rick-merges flow still governs
future changes unless Rick directs otherwise.
