# Task: Sign every release with one permanent key

This is planned feature 2 from `docs/PLANNED_FEATURES.md` ("Permanent release signing key"), with the answers to its open questions folded in. Version 1.5.2 carries the code change. It was never published: its certificate check read nothing from the newest `apksigner` on GitHub's runners. 1.5.3 pins `apksigner` to build-tools 35.0.0 and is the first signed release. The key itself and the checks that need it are the owner's, on the laptop.

## Context

Read `CLAUDE.md` first; its rules are binding. Android installs an update only when it's signed with the same certificate as the installed app. Until 1.5.1, CI signed every release with the debug key of a fresh GitHub runner, so each release had a different certificate. No release could install over the one before, and the only way past that was to uninstall, which deletes every school year on the phone. The planned-features entry lists the certificates of the releases up to 1.5.0.

## Goal

- Every release that CI builds on `main` is signed with one permanent key, so each new release installs as an update and keeps the school years.
- CI can't publish a release signed with any other key. A missing or wrong key fails the build on `main`, and the release workflow checks the certificate again before it publishes.
- Builds without the key (Android Studio, `installDebug`, a local `assembleRelease`) keep working and fall back to the debug key.
- The key and its password exist only on the owner's laptop, in the owner's two backups and in the repository's Actions secrets. They never appear in a chat, an AI session, an issue, a pull request or a commit.

## Decisions (from the open questions)

1. **Key type:** RSA 4096, signed SHA256withRSA, PKCS12, alias `release`, valid for 10,000 days (to about 2053). This is the `keytool` command in the planned-features entry, unchanged.
2. **Version:** PATCH, 1.5.2 (`versionCode` 12). The `CHANGELOG.md` rules call build and CI changes PATCH. The changelog section's first line spells out the one last uninstall.
3. **Debug builds beside the release:** yes. The debug build type gets `applicationIdSuffix = ".debug"` and the label "Course Pacer (debug)". Android Studio installs a separate app with its own data, which never clashes with or touches the real one. Release builds keep `com.coursebook.pacer`.
4. **Data on phones before the switch:** phones that run a GitHub release can't keep their data through the switch. Any new build, whatever it contains, is signed with a different key from the one on the phone, so installing it needs an uninstall. Those phones re-enter their school years once after installing 1.5.2. Export and import of the saved file comes next, as its own feature with its own brief and pull request, so that from then on the data can be saved before any reinstall. **Merge order:** this change first, then export and import, so export arrives as the first update that installs over 1.5.2.
5. **Fingerprint in the release notes:** yes. `release.yml` reads the certificate of each APK it attaches, refuses to publish if it isn't `SIGNING_CERT_SHA256`, and appends "Signed with certificate SHA-256 `…`" to the notes.
6. **Google Play later:** nothing to decide now. If the app moves to Play, use Play App Signing with this key as the upload key. The 10,000-day validity already meets Play's rule (valid past 22 October 2033).
7. **Where the work happens** (asked at the start of the session): the code change, the brief and the checks that need no key happen in a cloud session, which ends in a draft pull request. The owner creates the key and the secrets on the laptop (Phase 1) and runs the checks that need the key (Phase 3, steps 2, 4, 5 and 6) before the pull request leaves draft.

## What to build (Phase 2)

1. **`app/build.gradle.kts`:**
   - A `release` signing config exists only when the `SIGNING_KEYSTORE` environment variable is set and not blank. Read it with `providers.environmentVariable(…)`, which the configuration cache tracks.
   - `storeFile = file(SIGNING_KEYSTORE)`, `storeType = "pkcs12"`, `keyAlias = "release"`, and `storePassword` and `keyPassword` from `SIGNING_PASSWORD`. A keystore with a missing or empty password fails the configuration with a message that names `SIGNING_PASSWORD`, instead of signing with nothing.
   - The release build type uses `signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")`.
   - The debug build type sets `applicationIdSuffix = ".debug"`. `app/src/debug/res/values/strings.xml` overrides `app_name` with "Course Pacer (debug)". A build-type resource overrides the main one without the duplicate-resource error that `resValue` would raise.
2. **`.github/workflows/android.yml`, "Release APK" step:**
   - `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD` come from `secrets` as step environment variables.
   - With the keystore secret: fail if `SIGNING_PASSWORD` is empty. Otherwise decode the keystore into `$RUNNER_TEMP/release.p12` after stripping spaces and line breaks (`tr -d ' \r\n' | base64 -d`), export `SIGNING_KEYSTORE`, record `SIGNED_WITH_RELEASE_KEY=true` in `$GITHUB_ENV`, build, and delete the decoded file whether or not the build passed.
   - Without it on `refs/heads/main`: fail with an `::error::` that names the missing secret.
   - Without it anywhere else: a notice, then the debug key. Pull requests from forks get no secrets, so they land here and never publish.
3. **`.github/workflows/android.yml`, new step "Check the signing certificate"** after the build:
   - Run `apksigner verify --print-certs` on the APK, with the newest `$ANDROID_HOME/build-tools/*/apksigner` on the runner.
   - Write the `Signer #1 certificate SHA-256 digest` to the job summary.
   - If the release key was used, or the ref is `main`: fail when `vars.SIGNING_CERT_SHA256` is missing, when the APK has more than one signer, or when the digest differs from the variable. The variable is compared in lowercase without colons or spaces, so either form `keytool` and `apksigner` print works.
   - Otherwise the summary says the APK is debug-signed and not for release.
4. **`.github/workflows/release.yml`, "Create tag and release" step:** for each attached APK, read the digest the same way, fail before tagging if it isn't `SIGNING_CERT_SHA256`, and append "Signed with certificate SHA-256 `…`" to `notes.md`. A manual run that attaches no APK adds no line.
5. **`.gitignore`:** `*.p12` next to `*.jks` and `*.keystore`.
6. **Docs:**
   - `CLAUDE.md`: the `assembleRelease` comment, the signing Gotcha, the debug app ID, and the rules after it ships (from the planned-features entry).
   - `README.md`: the signing paragraph under "Build and run", and "Continuous integration" with the two secrets, the variable and the checks.
   - `docs/PLANNED_FEATURES.md`: entry 2's status points here.
   - `CHANGELOG.md`: a 1.5.2 section whose first line says that installing it needs one last uninstall, which deletes the school years on that phone, and that every release after it installs as an update.
7. **Version:** `versionName` 1.5.2, `versionCode` 12.

## Who does what

| Phase | Step | Who and where | Status |
| --- | --- | --- | --- |
| 0 | Settle the open questions | Owner, cloud session | Done (above) |
| 1 | Create the key, back it up twice, test both backups, add the two secrets and the variable | Owner, laptop | To do |
| 2 | Gradle, CI, `.gitignore`, docs, version | Cloud session, draft pull request | Done |
| 3.1 | Tests, `assembleRelease` and lint without the key; `apksigner` shows a debug certificate | Cloud session (repeat on the laptop if you like) | Done |
| 3.2 | `assembleRelease` with the real key; `apksigner` shows the recorded fingerprint | Owner, laptop | To do |
| 3.3 | The `main` guard fails without the secret and falls through to the debug key on a branch | Cloud session | Done |
| 3.4 | Re-run CI on the pull request once the secrets exist: green, and the job summary shows the recorded fingerprint | Owner, GitHub Actions ("Re-run all jobs") | To do |
| 3.5 | Download the pull request's `coursebook-pacer-apk` artifact; `apksigner` shows the same fingerprint | Owner, laptop | To do |
| 3.6 | Install that APK on an emulator (uninstall first); the app opens and keeps a school year across a restart | Owner, laptop | To do |
| 4 | Merge; check the release asset and its notes with `apksigner` | Owner | To do |
| 5 | Move each phone over once (uninstall, install 1.5.2, re-enter the years) | Owner | To do |
| 6 | The next release installs as an update with the data intact; then mark the entry shipped | Owner | To do |

Phase 1 and every check in Phase 3 must pass before the pull request leaves draft. Merging without the secrets fails CI on `main` by design, so nothing would be published, but `main` would stay red until they're added.

Phase 3.1 and 3.3 were also run in the cloud session with a throwaway test keystore (made there, never committed, deleted afterwards) to prove that the Gradle config signs with a supplied PKCS12 file, and that the certificate check passes on a match and fails on a mismatch. That key has nothing to do with the release key.

## Since 1.7.3: secrets only for `main`

Repository secrets reached every build, including branch pushes and pull requests from this repository, and a branch's own build files run with them in reach. The secrets now live in a GitHub Environment instead:

- **Environment** `release` (Settings → Environments), with deployment branches limited to `main` and no required reviewers. It holds `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD`. `SIGNING_CERT_SHA256` stays a repository variable: it is public.
- **`android.yml`** sets `environment: ${{ github.ref == 'refs/heads/main' && 'release' || '' }}` on the build job, so builds of `main` (pushes and manual runs) get the secrets and every other build gets none and falls back to the debug key, as forks always did. The workflow also sets `permissions: contents: read`.
- **Then** the repository-level copies of the two secrets are deleted, so nothing but `main` can read them.

## Out of scope

- Key rotation (APK Signature Scheme v3 lineage). Plan it as its own task if the key ever leaks.
- Export and import of school years: the next feature, with its own brief.
- Any change to the app's code, the engine or the saved file.
