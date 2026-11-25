# AGENTS.md — LocalTube

Repository-wide instructions for coding agents (ChatGPT, Claude, ChatGPT Codex)
working on **rickevans86/LocalTube**. This file combines the 3-AI collaboration
protocol with the Android conventions for this app. The `.ai/` files hold the
live project state; this file holds the rules.

**GitHub is the single source of truth.** Issues are authoritative for task
assignment and status; commits and PRs are authoritative for implemented
changes. If a `.ai/` file conflicts with GitHub history, GitHub wins — fix the
file.

---

## Project

- **Purpose:** An offline, kid-safe Android app that plays videos already on the
  device, presented in a YouTube Kids–style interface (big thumbnails, no
  search, no ads, no outbound links). Parent-curated content behind a parent
  gate, per-profile screen-time limits, and a kiosk lock. No accounts, no
  network — except an optional self-updater.
- **Stack** _(proposed; pending Rick's confirmation — see `.ai/DECISIONS.md`.
  Do not switch toolkit, architecture or major libraries without a new Issue and
  Rick's approval):_
  - Language: **Kotlin**
  - UI: **Jetpack Compose + Material 3**
  - Playback: **Media3 (ExoPlayer)** — local codecs, playlist auto-advance
  - Persistence: **Room** (metadata/profiles/history); **DataStore + Android
    Keystore** for prefs and the parent-gate hash
  - Content access: **Storage Access Framework** scoped tree URIs (minimum
    permissions)
  - DI: **Hilt**
  - Build: **Gradle (Kotlin DSL) + version catalog**; Gradle wrapper committed
  - **minSdk 26, targetSdk 35**
- **Commands** _(intended — the Gradle project is not scaffolded yet, so these
  will not run until Phase 0 scaffolding lands; update this section when the real
  commands differ):_
  - Install/open: import into Android Studio, or `./gradlew help`
  - Build debug: `./gradlew assembleDebug`
  - Install on device: `./gradlew installDebug`
  - Unit tests: `./gradlew test`
  - Instrumentation tests: `./gradlew connectedAndroidTest` _(needs a device/
    emulator; if none, mark "not run")_
  - Lint: `./gradlew lint` _(plus `./gradlew ktlintCheck` / `spotlessCheck` once
    a formatter is configured)_

---

## Authority

Rick assigns tasks and is the only one who merges into main. "Authorized" means
Rick has assigned the task to you — via a labelled GitHub Issue or in session.

## Team roles

- **ChatGPT** — architecture & planning, feature specs, task creation/priority.
- **Claude** — independent code review, analysis & debugging, architecture review.
- **Codex** — implementation, refactoring, automated tests, feature branches &
  PRs, repo maintenance.

Defaults, not exclusive restrictions: any agent may perform an authorized task it
has the tools for. **One rule is fixed: the AI that wrote a change does not
review it. Rick names the reviewer.**

## Shared memory (`.ai/`)

- `PROJECT_STATE.md` — current overview
- `TASK_BOARD.md` — summary of active Issues (rebuilt from Issues; don't
  hand-edit rows from memory)
- `AI_ACTIVITY_LOG.md` — who completed what (append-only; one dated entry at the
  end; never edit/reorder earlier entries)
- `HANDOFF.md` — most recent handoff only (replace contents; history lives in the
  activity log)
- `DECISIONS.md` — architectural decisions (append-only)
- `FILE_OWNERSHIP.md` — files for in-progress tasks only (add when you claim,
  remove in the completing PR)

Update the `.ai/` files as the **last** step before opening a PR, after bringing
your branch up to date with main. On a `.ai/` merge conflict, keep **both**
agents' entries — never discard another agent's.

## Session start

1. Identify yourself (ChatGPT / Claude / Codex).
2. Determine GitHub access; if none, follow Website Mode.
3. Retrieve the latest repo state.
4. Read this file and all relevant `.ai/` files.
5. Review open Issues, active PRs, recent commits.
6. Identify other agents' completed work.
7. Check `FILE_OWNERSHIP.md` and open PRs for overlap.
8. Confirm Rick's assigned task, or recommend the next unclaimed one and wait.

Never assume info from a previous conversation is current.

## Task ownership

Every task has a GitHub Issue with: ID/number, description, acceptance criteria,
responsible AI, status, dependencies, related branch/PR.

Labels: `ai:chatgpt` · `ai:claude` · `ai:codex` · `status:in-progress` ·
`status:blocked` · `status:review`.

Claiming: work only on an Issue Rick assigned you; check no other agent's label
and no overlapping open PR / `FILE_OWNERSHIP.md` entry; apply your `ai:` label +
`status:in-progress` and comment that you've claimed it; re-read to confirm. If
another agent claimed it or files overlap, stop and tell Rick. Found work with no
Issue? Propose a new Issue — don't fold it into your current task.

## Development workflow

1. Claim the task.
2. Branch from latest main: `<agent>/<issue-number>-<short-name>` (e.g.
   `codex/42-folder-scan`). One Issue per branch.
3. Implement only what the Issue asks.
4. Run the test and lint commands above.
5. Review the diff.
6. Commit with AI identity + task reference (format below).
7. Bring the branch up to date with main, then update the `.ai/` files.
8. Open a PR.
9. Set the Issue to `status:review` and request review.

Commit message format:

```
Add folder scan (#42)

AI: Codex
```

Never force-push shared branches or overwrite another agent's work. Don't push to
another agent's branch unless Rick asks. **Do not merge into main — Rick merges.**

## Attribution

Each development report identifies: AI responsible, task ID, work performed, files
changed, tests performed, commit SHA / PR link, outstanding issues. If tests were
not run, write "Not run" and the reason. Never attribute work to an AI that did
not perform it.

## Website Mode

Operating via the ChatGPT/Claude website with no repo access: state up front that
you can't see the repo; ask Rick for the files/Issue text/`.ai/` files you need;
state your output isn't checked against the current repo; prepare complete files
or a diff plus the commit message, activity-log entry and handoff text; never
claim a GitHub update succeeded unless it did.

---

## Android conventions

### Project layout
- Standard Gradle Android: `app/` module to start (`src/main`, `src/test`,
  `src/androidTest`).
- Commit the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`); agents
  use the wrapper, never machine-wide Gradle.
- `.gitignore` excludes `/build`, `/.gradle`, `/.idea` (except shared run configs
  if wanted), `local.properties`, `*.apk`, `*.aab`, and keystores.

### Build & test
Use the wrapper for everything (commands in the Project section). Before a PR,
run unit tests and lint and report each as passed / failed / "not run" + reason.
Never report instrumentation tests as passed if no device was available.

### Android rules
- **Permissions:** request the minimum a feature needs; call out any new manifest
  permission in the PR and why. (Known expected: `REQUEST_INSTALL_PACKAGES` for
  the self-updater.)
- **Secrets:** no keys/tokens/signing keys/passwords in the repo. Read from
  `local.properties`, Gradle properties, or env; document the names here. Found a
  committed secret? Tell Rick at once and don't repeat its value.
- **Signing & releases:** release signing is done by CI from main using the
  action secrets below — never on an agent's machine. Agents build debug. Never
  commit a keystore. Don't hand-edit `versionCode`/`versionName`; the release
  workflow derives them.
- **Compatibility:** stay within the recorded minSdk; a feature needing a higher
  API level is guarded or raises minSdk through its own Issue.
- **Dependencies:** add a library only when it earns its place; note new deps in
  the PR; prefer well-maintained, widely used libraries.
- **Resources & accessibility:** user-facing strings in `strings.xml`; content
  descriptions on interactive elements; dark-theme support.

### CI, releases & updates
- **main is the release line** — always releasable; every change merged is part
  of the next release. Development still goes branch → PR → Rick merges; CI cuts
  the release from main automatically on push.
- **Workflow:** `.github/workflows/release.yml` on push to main — checkout, set
  up JDK, `./gradlew` release build, sign with the keystore from secrets, create
  and publish a GitHub Release with the APK attached. **Prefer a self-hosted
  runner:** `runs-on: [self-hosted, linux, android]` _(confirm Rick's runner
  labels and record them here)_; document an `ubuntu-latest` fallback.
- **Versioning (derived, never hand-edited):** `versionCode` from the CI run
  number / commit count; `versionName` from the release tag; tag each release
  (e.g. `v1.0.<run_number>`).
- **Action secrets** (names only — never commit values; set via
  `gh secret set <NAME> --repo rickevans86/LocalTube`):
  - `ANDROID_KEYSTORE_BASE64` — release keystore, base64-encoded
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

  The workflow decodes `ANDROID_KEYSTORE_BASE64` to a temp file, signs, and
  deletes it; the keystore never enters the repo.
- **Built-in updater:** on launch and from a manual "Check for updates", query
  the Releases API for the latest release, compare tag/`versionCode` to the
  installed `versionCode`, and if newer offer to download the APK and hand it to
  `PackageInstaller` (needs `REQUEST_INSTALL_PACKAGES`). Handle no-network and
  failed downloads quietly. Keep owner/repo/release URL in one config constant.
  Keep the repo **public** so no token is needed; raise with Rick if it must be
  private.

### What an Android Issue should contain
Beyond the protocol fields: the screen/component affected; any new permission,
dependency or minSdk change expected; how to verify by hand on a device/emulator
alongside the automated tests.

### Definition of done
- Builds with `./gradlew assembleDebug`.
- Unit tests and lint run, results reported honestly.
- No new secret/permission/dependency slipped in without being called out.
- This file updated if build/test/run commands changed.
- If the change touches the release workflow or updater, the workflow still
  builds, signs and publishes from main, and the updater can still read it.
- PR opened with the protocol's template; Rick merges; CI publishes from main.

---

## Critical rules
- Never invent project progress; report only verified work you did.
- Never claim a GitHub update succeeded without confirmation.
- Never expose secrets in commits, Issues, PRs or logs.
- Always preserve project history; prefer small, reviewable PRs.
- Ask Rick when task ownership is ambiguous.
