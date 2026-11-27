#!/usr/bin/env fish
# Usage: fish scripts/sign-release.fish INPUT.apk EXPECTED_SHA256 OUTPUT.apk
# The signing password is requested interactively by apksigner.
if test (count $argv) -ne 3
    echo "Usage: sign-release.fish INPUT.apk EXPECTED_SHA256 OUTPUT.apk" >&2
    exit 2
end
set -l sdk "$HOME/Android/Sdk/build-tools/37.0.0"
set -l key "$HOME/.local/share/aurora-signing/aurora-release.p12"
set -l input "$argv[1]"
set -l expected "$argv[2]"
set -l output "$argv[3]"
set -l cert 2cf99dbb9cd5f2f5e34ce28b383ee198824bac85b1d5ca129cc1a2552dd6da4f
if test -e "$output"
    echo "Output already exists; refusing to overwrite." >&2
    exit 1
end
set -l actual (sha256sum -- "$input" | string split -m 1 ' ')[1]
test "$actual" = "$expected"; or begin
    echo "SHA256 mismatch" >&2
    exit 1
end
set -l work (mktemp -d)
or exit 1
function cleanup --on-event fish_exit --inherit-variable work
    rm -rf -- "$work"
end
"$sdk/zipalign" -P 16 -f 4 "$input" "$work/aligned.apk"; or exit 1
"$sdk/apksigner" sign --ks "$key" --ks-type PKCS12 --ks-key-alias aurora-release --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false --out "$work/signed.apk" "$work/aligned.apk"; or exit 1
set -l report ("$sdk/apksigner" verify --verbose --print-certs "$work/signed.apk")
or exit 1
printf '%s\n' $report | string match -q "*certificate SHA-256 digest: $cert"; or begin
    echo "Unexpected signing certificate" >&2
    exit 1
end
"$sdk/zipalign" -c -P 16 4 "$work/signed.apk"; or exit 1
cp --no-clobber -- "$work/signed.apk" "$output"; or exit 1
sha256sum -- "$output"
