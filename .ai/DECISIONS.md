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
