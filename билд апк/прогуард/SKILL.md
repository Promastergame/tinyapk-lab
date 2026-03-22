---
name: proguard-r8-without-gradle
description: Run R8/ProGuard on Android APK without Gradle — shrink, obfuscate and minify DEX using R8 that's already inside d8.jar
version: 1.0
author: Promaster
---

# SKILL: ProGuard / R8 Without Gradle

## Key insight: R8 is already inside d8.jar!

You do NOT need to download ProGuard or R8 separately. Google's R8 compiler (which replaced ProGuard in all modern Android builds) is bundled inside `d8.jar` from the Android SDK build-tools. Same jar, different main class:

```bash
# D8 mode — just converts .class → .dex (no shrinking)
java -cp d8.jar com.android.tools.r8.D8 ...

# R8 mode — shrinks + obfuscates + converts .class → .dex
java -cp d8.jar com.android.tools.r8.R8 ...
```

R8 does everything ProGuard does plus DEX conversion in one step — no separate ProGuard pass needed.

---

## What R8 does

| Feature | Result |
|---------|--------|
| Tree shaking | Removes unused classes, methods, fields |
| Obfuscation | Renames classes/methods to `a`, `b`, `c`... |
| Minification | Removes debug info, line numbers |
| DEX conversion | Outputs `classes.dex` directly |

Typical size reduction on a real app: **30-50%**. On already-minimal apps (like a 16KB Tetris): **15-27%** — most of the weight is APK structure and signing overhead.

---

## ProGuard config file

Create `proguard.pro` — tells R8 what to keep (don't obfuscate entry points):

```proguard
# Keep all Android entry points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.view.View
-keep public class * extends android.view.SurfaceView

# Keep lifecycle methods on Activities
-keepclassmembers class * extends android.app.Activity {
    protected void onCreate(android.os.Bundle);
    protected void onDestroy();
    protected void onPause();
    protected void onResume();
    protected void onActivityResult(int, int, android.content.Intent);
}

# Keep SurfaceView callbacks
-keepclassmembers class * extends android.view.SurfaceView {
    public void surfaceCreated(android.view.SurfaceHolder);
    public void surfaceDestroyed(android.view.SurfaceHolder);
    public void surfaceChanged(android.view.SurfaceHolder, int, int, int);
    public boolean onTouchEvent(android.view.MotionEvent);
}

# Keep View constructors (needed for inflation)
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Don't warn about missing classes from android.jar stubs
-dontwarn **

# Allow R8 to make members more accessible for optimization
-allowaccessmodification

# Skip preverification (not needed for Android/DEX)
-dontpreverify
```

### Rules explained

| Rule | Why needed |
|------|-----------|
| `-keep class * extends Activity` | Android calls `onCreate` by reflection — can't rename |
| `-keepclassmembers ... onCreate` | Same — lifecycle called by OS, not your code |
| `-dontwarn` | `android.jar` has stub classes — R8 would warn about everything |
| `-allowaccessmodification` | Lets R8 make private methods public for better inlining |
| `-dontpreverify` | Java preverification is useless on Android/DEX |

### What NOT to put in config (R8 handles these automatically)
- `-optimizationpasses` — R8 ignores this (it's a ProGuard-only option)
- `-optimizations` — R8 ignores this too
- `-dontobfuscate` — only add this if you need readable crash logs

---

## Build command

Replace the `D8` step from the normal build with `R8`:

```bash
ANDROID_JAR=/path/to/sdk/platforms/android-34/android.jar
BUILD=/path/to/build

# Instead of D8:
# java -cp d8.jar com.android.tools.r8.D8 --output $BUILD/dex --lib $ANDROID_JAR $(find $BUILD/classes -name "*.class")

# Use R8:
java -cp d8.jar com.android.tools.r8.R8 \
  --release \
  --min-api 26 \
  --lib $ANDROID_JAR \
  --output $BUILD/dex \
  --pg-conf proguard.pro \
  $(find $BUILD/classes -name "*.class")
```

### R8 flags

| Flag | Meaning |
|------|---------|
| `--release` | Strips debug info (line numbers, variable names) |
| `--min-api 26` | Target Android API level — affects desugaring |
| `--lib` | Android platform jar (needed for type resolution) |
| `--output` | Output directory for `classes.dex` |
| `--pg-conf` | ProGuard config file |
| `--no-minification` | Disable name obfuscation (keeps readable names) |
| `--no-tree-shaking` | Disable dead code removal |
| `--pg-map-output` | Output mapping file (for deobfuscating crash logs) |

---

## Full release build script

Complete script replacing debug D8 → release R8:

```bash
#!/bin/bash
# release-build.sh — builds a release APK with R8 obfuscation

AAPT2=./tools/aapt2
ANDROID_JAR=./tools/android.jar
D8_JAR=./tools/d8.jar
APKSIGNER_JAR=./tools/apksigner.jar
PROJ=./app/src/main
BUILD=./build_release
KEYSTORE=./release.keystore

mkdir -p $BUILD/res $BUILD/classes $BUILD/dex $BUILD/gen $BUILD/apk $BUILD/apk_final

# 1. Compile resources
$AAPT2 compile --dir $PROJ/res -o $BUILD/res/compiled.zip

# 2. Link resources
$AAPT2 link \
  -o $BUILD/apk/resources.apk \
  -I $ANDROID_JAR \
  --manifest $PROJ/AndroidManifest.xml \
  --java $BUILD/gen \
  --auto-add-overlay \
  $BUILD/res/compiled.zip

# 3. Compile Java — pass 1 (game logic + R.java)
java -jar ecj.jar -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath $ANDROID_JAR \
  -classpath $ANDROID_JAR \
  -d $BUILD/classes \
  $(find $PROJ/java -name "*.java" | grep -v "ui/") \
  $BUILD/gen/com/mygame/R.java

# 4. Compile Java — pass 2 (UI classes)
java -jar ecj.jar -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath $ANDROID_JAR \
  -classpath "$ANDROID_JAR:$BUILD/classes" \
  -d $BUILD/classes \
  $(find $PROJ/java -name "*.java" | grep "ui/")

# 5. R8 — shrink + obfuscate + convert to DEX  ← THE KEY STEP
java -cp $D8_JAR com.android.tools.r8.R8 \
  --release \
  --min-api 26 \
  --lib $ANDROID_JAR \
  --output $BUILD/dex \
  --pg-conf proguard.pro \
  --pg-map-output $BUILD/mapping.txt \
  $(find $BUILD/classes -name "*.class")

# 6. Package
cp $BUILD/apk/resources.apk $BUILD/apk_final/app-unsigned.apk
cd $BUILD/dex && zip -u $BUILD/apk_final/app-unsigned.apk classes.dex
cd -

# 7. Align
zipalign -f 4 $BUILD/apk_final/app-unsigned.apk $BUILD/apk_final/app-aligned.apk

# 8. Sign (create keystore first if needed — see below)
java -jar $APKSIGNER_JAR sign \
  --ks $KEYSTORE \
  --ks-key-alias releasekey \
  --ks-pass pass:YOUR_PASSWORD \
  --key-pass pass:YOUR_PASSWORD \
  --out $BUILD/apk_final/MyApp-release.apk \
  $BUILD/apk_final/app-aligned.apk

echo "Done! APK: $BUILD/apk_final/MyApp-release.apk"
echo "Mapping: $BUILD/mapping.txt (keep this for crash deobfuscation)"
```

---

## Release keystore (for Play Store)

Debug keystore is fine for testing but Play Store needs a real release keystore:

```bash
# Create release keystore (do this ONCE, keep it safe forever!)
keytool -genkey -v \
  -keystore release.keystore \
  -alias releasekey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 25000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Your Name, OU=Your Org, O=Your Company, L=City, ST=State, C=US"
```

**WARNING:** Never lose `release.keystore`! If you lose it, you can never update your app on Play Store. Back it up to multiple places.

---

## Mapping file — deobfuscating crash logs

R8 renames everything to `a`, `b`, `c`... so crash logs look like:

```
at a.b.c(Unknown Source:3)
at a.a.a(Unknown Source:11)
```

Use `--pg-map-output mapping.txt` and keep the mapping file. To deobfuscate:

```bash
# retrace is also inside d8.jar
java -cp d8.jar com.android.tools.r8.retrace.Retrace \
  mapping.txt \
  stacktrace.txt
```

---

## Real-world size comparison

Results from Tetris (16KB) and Sandbox (21KB) projects:

| Project | Debug DEX | Release DEX | Saving |
|---------|-----------|-------------|--------|
| Tetris | 15KB | 11KB | -27% |
| Sandbox | 17KB | 13KB | -24% |

On larger projects (typical app ~2-5MB):

| Project type | Before R8 | After R8 | Saving |
|-------------|-----------|----------|--------|
| Simple app | 2MB | 1.2MB | ~40% |
| App + libraries | 5MB | 2.8MB | ~44% |
| App + many libs | 12MB | 6MB | ~50% |

The smaller the project, the less R8 saves — most of the APK weight in tiny projects is APK structure, signing, and resources, not code.

---

## Common errors and fixes

| Error | Fix |
|-------|-----|
| `Ignoring option: -optimizationpasses` | Normal — R8 ignores ProGuard-only options, not a problem |
| `rule does not match anything` | Normal if your app has no Services — remove that `-keep` rule |
| App crashes after R8 | Something got obfuscated that shouldn't — add `-keep` rule for that class |
| `ClassNotFoundException` at runtime | Add `-keep class com.yourpackage.YourClass` to proguard.pro |
| Callbacks not called | Add `-keepclassmembers` for that interface/method |
| Can't read crash logs | Use `Retrace` tool with your `mapping.txt` file |
| `--pg-compat` flag | Use only if you need exact ProGuard behavior — R8 is better without it |
