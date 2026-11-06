#!/usr/bin/env bash
set -euo pipefail
project="$(cd -- "$(dirname -- "$0")" && pwd)"
sdk_path="${RIN23_ANDROID_SDK:-${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}}"
if [[ -z "$sdk_path" ]]; then echo 'Set RIN23_ANDROID_SDK to an Android SDK with platform 35 and build-tools 35.0.0' >&2; exit 1; fi
bt="$sdk_path/build-tools/35.0.0"
platform="$sdk_path/platforms/android-35/android.jar"
output="$project/build"
version_name=$(sed -n 's/.*NAME="\([^"]*\)".*/\1/p' "$project/app/src/main/java/com/rin23/app/AppVersion.java")
version_code=$(sed -n 's/.*CODE=\([0-9]*\).*/\1/p' "$project/app/src/main/java/com/rin23/app/AppVersion.java")
[[ "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-beta)?$ && "$version_code" =~ ^[0-9]+$ ]] || exit 1
rm -rf -- "$output/classes" "$output/generated" "$output/dex" "$output/res"
rm -f -- "$output/app-classes.jar"
mkdir -p "$output/res" "$output/generated" "$output/classes" "$output/dex"
python3 "$project/tools/generate_security_config.py"
"$bt/aapt2" compile --dir "$project/app/src/main/res" -o "$output/res/compiled.zip"
"$bt/aapt2" link -I "$platform" -A "$project/app/src/main/assets" --manifest "$project/app/src/main/AndroidManifest.xml" --java "$output/generated" --proguard "$output/aapt-rules.pro" --version-code "$version_code" --version-name "$version_name" -o "$output/base.apk" "$output/res/compiled.zip"
sources=("$project/app/src/main/java/com/rin23/app/"*.java "$output/generated/com/rin23/app/"*.java)
java com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$platform:$project/deps/zxing-core-3.5.3.jar" -d "$output/classes" "${sources[@]}"
(cd "$output/classes" && zip -q -r "$output/app-classes.jar" .)
# Build-tools' D8 jar includes R8. No unobfuscated release fallback.
java -cp "$bt/lib/d8.jar" com.android.tools.r8.R8 --release --lib "$platform" --min-api 29 \
 --pg-conf "$project/proguard-rules.pro" --pg-conf "$output/aapt-rules.pro" \
 --pg-map-output "$output/mapping.txt" --output "$output/dex" \
 "$output/app-classes.jar" "$project/deps/zxing-core-3.5.3.jar"
cp "$output/base.apk" "$output/unsigned.apk"
(cd "$output/dex" && zip -q -j "$output/unsigned.apk" classes*.dex)
mkdir -p "$output/native/lib/arm64-v8a"
cp "$project/app/src/main/jniLibs/arm64-v8a/librinengine.so" "$output/native/lib/arm64-v8a/librinengine.so"
(cd "$output/native" && zip -q "$output/unsigned.apk" lib/arm64-v8a/librinengine.so)
"$bt/zipalign" -f -P 16 4 "$output/unsigned.apk" "$output/aligned.apk"
if [[ -n "${RIN23_KEYSTORE:-}" ]]; then
 "$bt/apksigner" sign --ks "$RIN23_KEYSTORE" --ks-key-alias "${RIN23_KEY_ALIAS:-rin23}" --ks-pass env:RIN23_STORE_PASSWORD --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true --out "$output/Rin23-v${version_name}-arm64.apk" "$output/aligned.apk"
 "$bt/apksigner" verify --verbose "$output/Rin23-v${version_name}-arm64.apk"
else
 echo 'Unsigned build ready: build/aligned.apk. Set RIN23_KEYSTORE and RIN23_STORE_PASSWORD to sign.'
fi
