#!/bin/bash

set -e

AAPT2=./aapt2.exe
ANDROID_JAR=./android.jar
ECJ_JAR=./ecj-3.45.0.jar
D8_JAR=./d8.jar
ZIPALIGN=./zipalign.exe
APKSIGNER_JAR=./apksigner.jar
KEYSTORE=./promaster.keystore
KEY_ALIAS=promaster
KEY_PASS=promaster
PROJ=./app/src/main
BUILD=./build

echo "Building Sandbox APK (LowSand)..."

rm -rf "$BUILD"
mkdir -p "$BUILD/res" "$BUILD/classes" "$BUILD/dex" "$BUILD/apk" "$BUILD/apk_final"

echo "Link manifest/resources..."
if find "$PROJ/res" -type f | grep -q .; then
  "$AAPT2" compile --dir "$PROJ/res" -o "$BUILD/res/compiled.zip"
  "$AAPT2" link \
    -o "$BUILD/apk/resources.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$PROJ/AndroidManifest.xml" \
    --auto-add-overlay \
    "$BUILD/res/compiled.zip"
else
  "$AAPT2" link \
    -o "$BUILD/apk/resources.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$PROJ/AndroidManifest.xml"
fi

echo "Compile Java..."
mapfile -t JAVA_FILES < <(find "$PROJ/java" -name "*.java")
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -classpath "$ANDROID_JAR" \
  -d "$BUILD/classes" \
  "${JAVA_FILES[@]}"

echo "Run R8..."
mapfile -t CLASSES < <(find "$BUILD/classes" -name "*.class")
java -cp "$D8_JAR" com.android.tools.r8.R8 \
  --release \
  --dex \
  --min-api 26 \
  --lib "$ANDROID_JAR" \
  --pg-conf ./proguard.pro \
  --pg-map-output "$BUILD/mapping.txt" \
  --output "$BUILD/dex" \
  "${CLASSES[@]}"

echo "Package APK..."
cp "$BUILD/apk/resources.apk" "$BUILD/apk_final/app-unsigned.apk"
(cd "$BUILD/dex" && zip -q -u "../apk_final/app-unsigned.apk" classes.dex)

echo "Zipalign..."
"$ZIPALIGN" -f 4 "$BUILD/apk_final/app-unsigned.apk" "$BUILD/apk_final/app-aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
    echo "Create Promaster keystore..."
    keytool -genkeypair \
      -alias "$KEY_ALIAS" \
      -keyalg EC \
      -groupname secp256r1 \
      -sigalg SHA256withECDSA \
      -validity 10000 \
      -keystore "$KEYSTORE" \
      -storetype PKCS12 \
      -storepass "$KEY_PASS" \
      -keypass "$KEY_PASS" \
      -dname "CN=Promaster,O=Promaster Development,C=US"
fi

echo "Sign APK..."
java -jar "$APKSIGNER_JAR" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass pass:"$KEY_PASS" \
  --key-pass pass:"$KEY_PASS" \
  --v1-signing-enabled false \
  --v2-signing-enabled true \
  --v3-signing-enabled false \
  --v4-signing-enabled false \
  --out "$BUILD/LowSand-release.apk" \
  "$BUILD/apk_final/app-aligned.apk"

cp "$BUILD/LowSand-release.apk" "$BUILD/LowSand.apk"

SIZE=$(wc -c < "$BUILD/LowSand-release.apk")
echo ""
echo "Done: LowSand-release.apk - ${SIZE} bytes"
