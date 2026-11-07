#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"

SDK=/opt/android-sdk
JAR=$SDK/platforms/android-29/android.jar
BUILD=$SDK/build-tools/35.0.0
OUT=build

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

/usr/bin/aapt2 compile --dir app/res -o "$OUT/res.zip"
/usr/bin/aapt2 link -o "$OUT/app-unsigned.apk" \
  -I "$JAR" \
  --manifest app/AndroidManifest.xml \
  --java "$OUT/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 35 \
  "$OUT/res.zip"

javac --release 11 -encoding UTF-8 -cp "$JAR" -d "$OUT/classes" \
  $(find app/src "$OUT/gen" -name '*.java')

"$BUILD/d8" --min-api 26 --output "$OUT/dex" \
  $(find "$OUT/classes" -name '*.class')

cd "$OUT"
zip -q app-unsigned.apk -j dex/classes.dex
/usr/bin/zipalign -f -p 4 app-unsigned.apk app-aligned.apk
keytool -genkeypair -keystore debug.keystore \
  -storepass android -keypass android -alias chadvidcraft \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Chad VidCraft, OU=Demo, O=ChadVidCraft, L=Local, ST=Local, C=US" >/dev/null 2>&1
/usr/bin/apksigner sign --ks debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out Chad-VidCraft.apk app-aligned.apk
/usr/bin/apksigner verify --verbose Chad-VidCraft.apk
cp Chad-VidCraft.apk ../Chad-VidCraft.apk
