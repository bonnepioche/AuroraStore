# Aurora AAS release procedure

The private signing key stays on the owner's PC. CI builds unsigned APKs, runs JVM and Keystore emulator tests, and prints the unsigned APK SHA-256. Never upload the keystore or passphrase.

1. Download the successful CI artifact and verify the SHA-256 from the job log.
2. Run `fish scripts/sign-release.fish INPUT.apk EXPECTED_SHA256 AuroraAAS-79-signed.apk` (change 79 to the exact versionCode).
3. Test the locally signed APK over the previous release, without uninstalling it.
4. Only after explicit owner approval, publish a normal, non-draft, non-prerelease GitHub Release on this fork, tag `aas-v0.4`, with exactly one asset named `AuroraAAS-79-signed.apk`. GitHub must report the asset's SHA-256 digest. Do not publish unsigned builds in GitHub Releases.
5. The in-app checker reads the latest public release. It accepts a higher version code only, downloads only the matching repository asset, validates size and SHA-256, checks Android package identity and both installed and downloaded signer fingerprints. The Android package installer performs final signature verification and requires user confirmation.

The initial V0.4 update from V0.3 requires local signing and ADB. Later updates may use Settings or About > Aurora AAS updates. Manual checks only; no background update check in this iteration. No Google credentials are passed to the GitHub client. A release must have a valid tag of form aas-vN.N or aas-vN.N.N.

Unresolved: actual signed update path on a Quintus, long-term session renewal, full gplayapi source audit. Archive parsing is not a substitute for Android's installation-time APK signature verification. Existing credential encryption format and signing identity are unchanged.
