# Release signing

> Decision record: built in 1.5.3; secrets limited to `main` since 1.7.3. The code and CLAUDE.md describe the current app.

Android installs an update only when it is signed with the same certificate as the installed app. Until 1.5.1, every release was signed with a different key, so none could install over the last, and the only way past that was an uninstall, which deletes the school years on the phone. Since 1.5.3 every release is signed with one permanent key.

## How it works

- **One permanent key.** CI signs release builds with it. The keystore and its password are secrets, and they are never committed or pasted anywhere.
- **Only `main` gets the secrets.** `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD` live in the `release` environment, which only `main` may use. A build of `main` without them fails, so a debug-signed APK is never published.
- **Other branches and pull requests** get no secrets. They build with the debug key and never publish.
- **The certificate is checked twice.** `android.yml` compares the APK's certificate with the `SIGNING_CERT_SHA256` variable and fails on a mismatch, a missing variable or more than one signer. `release.yml` checks it again before it tags, and adds the certificate to the release notes.
- **Builds without the key** (Android Studio, `installDebug`, a local `assembleRelease`) use the debug key. Debug builds are a separate app, `gr.edusuite.pacer.debug`, with their own data.

## Rules

- Never change the key, `keyAlias`, the secret names or the variable without the owner. A new key means every phone has to uninstall, which deletes its school years.
- If the key leaks, rotate it with APK Signature Scheme v3 (`apksigner rotate` and a signing lineage). Plan that as its own task.
