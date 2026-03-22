---
name: build-android-apk-without-gradle
description: Build Android APK without Android Studio, Gradle, or internet
version: 1.0
author: Promaster
---

# SKILL: Build Android APK Without Gradle

## When to use
Use this skill when you need to build an Android APK on a Linux server WITHOUT Android Studio, WITHOUT Gradle, and WITHOUT internet access to Google/Maven servers.

This skill was born from a real session where all standard approaches failed and the APK was built manually tool by tool.

---

## Tools needed

| Tool | Purpose | How to get |
|------|---------|------------|
| `aapt2` | Compile XML resources | `git clone github.com/rendiix/termux-aapt` → prebuilt x86-64 binary |
| `android.jar` | Android platform API classes | `git clone github.com/Reginer/aosp-android-jar` → pick API level |
| `ecj.jar` | Java compiler (replaces javac!) | Maven Central: `org.eclipse.jdt:ecj` — user uploads to chat |
| `d8.jar` | Java .class → DEX format | Android SDK: `build-tools/<ver>/lib/d8.jar` — user uploads |
| `apksigner.jar` | Sign the APK | Android SDK: `build-tools/<ver>/lib/apksigner.jar` — user uploads |
| `zipalign` | Align APK bytes | `apt-get install -y zipalign` |
| `keytool` | Create debug keystore | Bundled with JRE — no install needed |
| `java` (JRE only) | Run all jar tools | Already on server — verify with `java -version` |

### Why ECJ instead of javac?
`javac` is part of the JDK which is often unavailable or broken on restricted servers. ECJ (Eclipse Compiler for Java) is a standalone `.jar` that does the same job using only JRE. Always use `--release 8` flag for Android compatibility — using `-8` causes `LambdaMetafactory cannot be resolved` errors.

### Why .aar files contain classes.jar?
Android library packages (`.aar`) are just ZIP archives. The actual compiled classes live inside as `classes.jar`. Extract with:
```bash
unzip mylib.aar classes.jar -d mylib-extracted/
```
Find `.aar` files in user's Gradle cache at:
```
C:\Users\<name>\.gradle\caches\modules-2\files-2.1\androidx.<lib>\<lib>\<version>\<hash>\<lib>.aar
```

### What can be cloned from GitHub (no auth needed)
```bash
# android.jar for all API levels (2.2GB total — use sparse clone for one version)
git clone --depth=1 https://github.com/Reginer/aosp-android-jar.git
# then copy: android-jar/android-34/android.jar

# aapt2 prebuilt binary for Linux x86-64
git clone --depth=1 https://github.com/rendiix/termux-aapt.git
# binary at: termux-aapt/prebuilt-binary/x86-64/aapt2

# gradle-wrapper.jar (needed if using gradlew)
git clone --depth=1 --filter=blob:none --sparse https://github.com/gradle/gradle.git
cd gradle && git sparse-checkout set gradle/wrapper
# jar at: gradle/wrapper/gradle-wrapper.jar
```

---

## Build steps (in order)

### Step 1: Setup directory structure
```bash
mkdir -p /home/claude/sdk/platforms/android-34
mkdir -p /home/claude/sdk/build-tools/34.0.0/lib
mkdir -p /home/claude/build/{res,classes,dex,gen,apk,apk_final}
mkdir -p /home/claude/androidx-libs

# Place tools
cp android.jar        /home/claude/sdk/platforms/android-34/
cp d8.jar             /home/claude/sdk/build-tools/34.0.0/lib/
cp apksigner.jar      /home/claude/sdk/build-tools/34.0.0/lib/
cp aapt2              /home/claude/sdk/build-tools/34.0.0/
chmod +x              /home/claude/sdk/build-tools/34.0.0/aapt2

# Required package.xml for the platform
cat > /home/claude/sdk/platforms/android-34/package.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02">
<localPackage path="platforms;android-34" obsolete="false">
<type-details xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:platformType">
<api-level>34</api-level><codename></codename>
</type-details>
<revision><major>3</major></revision>
<display-name>Android SDK Platform 34</display-name>
</localPackage>
</ns2:repository>
EOF
```

### Step 2: Extract classes.jar from .aar files
```bash
LIBS=/home/claude/androidx-libs
for lib in appcompat core recyclerview; do
  unzip -o $LIBS/$lib.aar classes.jar -d $LIBS/$lib-extracted/
done
```

### Step 3: Compile XML resources with aapt2
```bash
AAPT2=/home/claude/sdk/build-tools/34.0.0/aapt2
ANDROID_JAR=/home/claude/sdk/platforms/android-34/android.jar
PROJ=/path/to/app/src/main
BUILD=/home/claude/build

# Compile all res/ files into a zip
$AAPT2 compile --dir $PROJ/res -o $BUILD/res/compiled.zip

# Link → produces resources.apk + R.java
# IMPORTANT: AndroidManifest.xml MUST have package="com.yourpackage" attribute
$AAPT2 link \
  -o $BUILD/apk/resources.apk \
  -I $ANDROID_JAR \
  --manifest $PROJ/AndroidManifest.xml \
  --java $BUILD/gen \
  --auto-add-overlay \
  $BUILD/res/compiled.zip
```

**Theme pitfall:** `Theme.AppCompat.*` requires androidx jars at link time and will fail. Use built-in Android themes:
```xml
<!-- GOOD - works without extra jars -->
<style name="AppTheme" parent="android:Theme.Material.Light.NoActionBar"/>

<!-- BAD - fails at aapt2 link step -->
<style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar"/>
```

**Manifest pitfall:** aapt2 requires `package` attribute even though AGP deprecates it:
```xml
<manifest xmlns:android="..." package="com.yourpackage">
```

### Step 4: Compile Java with ECJ (not javac!)
```bash
ANDROID_JAR=/home/claude/sdk/platforms/android-34/android.jar
LIBS=/home/claude/androidx-libs
BUILD=/home/claude/build

CP="$ANDROID_JAR"
CP="$CP:$LIBS/appcompat-extracted/classes.jar"
CP="$CP:$LIBS/core-extracted/classes.jar"
CP="$CP:$LIBS/recyclerview-extracted/classes.jar"

# Collect ALL java files + generated R.java into one list
find /path/to/app/src/main/java -name "*.java" > /tmp/sources.txt
echo "$BUILD/gen/com/yourpackage/R.java" >> /tmp/sources.txt

# Compile everything in ONE command
# CRITICAL: --release 8 (not -8) to avoid LambdaMetafactory error
# CRITICAL: all files at once so cross-file references resolve
java -jar /home/claude/ecj.jar \
  --release 8 \
  -encoding UTF-8 \
  -bootclasspath $ANDROID_JAR \
  -classpath $CP \
  -d $BUILD/classes \
  @/tmp/sources.txt
```

### Step 5: Convert .class → DEX with d8
```bash
BUILD=/home/claude/build
ANDROID_JAR=/home/claude/sdk/platforms/android-34/android.jar

java -cp /home/claude/sdk/build-tools/34.0.0/lib/d8.jar \
  com.android.tools.r8.D8 \
  --output $BUILD/dex \
  --lib $ANDROID_JAR \
  $(find $BUILD/classes -name "*.class")
```

### Step 6: Package APK
```bash
BUILD=/home/claude/build
PROJ=/path/to/app/src/main

# Base = resources.apk (already has manifest + compiled resources)
cp $BUILD/apk/resources.apk $BUILD/apk_final/app-unsigned.apk

# Add DEX
cd $BUILD/dex && zip -u $BUILD/apk_final/app-unsigned.apk classes.dex

# Add assets if project has them
cd $PROJ && zip -ur $BUILD/apk_final/app-unsigned.apk assets/
```

### Step 7: Zipalign
```bash
zipalign -v 4 \
  /home/claude/build/apk_final/app-unsigned.apk \
  /home/claude/build/apk_final/app-aligned.apk
```

### Step 8: Sign APK
```bash
# Create debug keystore (only needed once)
keytool -genkey -v \
  -keystore /home/claude/debug.keystore \
  -alias androiddebugkey \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"

# Sign
java -jar /home/claude/sdk/build-tools/34.0.0/lib/apksigner.jar sign \
  --ks /home/claude/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out /home/claude/build/apk_final/app-debug.apk \
  /home/claude/build/apk_final/app-aligned.apk
```

---

## Files to request from user

If tools are missing, ask the user to upload these files from their Windows PC:

```
# d8.jar and apksigner.jar:
C:\Users\<name>\AppData\Local\Android\Sdk\build-tools\<version>\lib\d8.jar
C:\Users\<name>\AppData\Local\Android\Sdk\build-tools\<version>\lib\apksigner.jar

# androidx .aar files (find version folders and look for .aar not .pom):
C:\Users\<name>\.gradle\caches\modules-2\files-2.1\androidx.appcompat\appcompat\1.6.1\<hash>\appcompat-1.6.1.aar
C:\Users\<name>\.gradle\caches\modules-2\files-2.1\androidx.recyclerview\recyclerview\1.3.2\<hash>\recyclerview-1.3.2.aar
C:\Users\<name>\.gradle\caches\modules-2\files-2.1\androidx.core\core\<version>\<hash>\core-<version>.aar

# ECJ Java compiler jar (download from search.maven.org → org.eclipse.jdt:ecj):
ecj-<version>.jar
```

---

## Common errors and fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `LambdaMetafactory cannot be resolved` | Used `-8` flag in ECJ | Use `--release 8` instead |
| `R cannot be resolved` | Compiled files separately | Compile ALL java files in ONE ecj command |
| `Theme.AppCompat not found` | AppCompat theme in aapt2 link | Use `android:Theme.Material.Light.NoActionBar` |
| `<manifest> must have package attribute` | Removed package from manifest | Add `package="com.yourpackage"` to manifest tag |
| `@NonNull cannot be resolved` | Missing androidx.annotation jar | Remove all `@NonNull` annotations and imports |
| `AppCompatActivity cannot be resolved` | Missing appcompat or annotation | Extend plain `Activity` instead |
| `NotificationCompat cannot be resolved` | Missing appcompat | Use native `Notification.Builder(ctx, channelId)` |
| `ActivityResultLauncher cannot be resolved` | Missing androidx.activity | Use `startActivityForResult` + `onActivityResult` |
| `java.lang.Object cannot be resolved` | Wrong bootclasspath syntax | Use `-bootclasspath $ANDROID_JAR` as separate flag |
| `sdk.dir not found` | Missing local.properties | Create file with `sdk.dir=/path/to/sdk` |
| Gradle downloads itself and fails | services.gradle.org blocked | Need gradle-wrapper.jar + pre-installed gradle |
| `duplicate class kotlin.collections` | Kotlin stdlib conflict in Gradle | Add `exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'` to build.gradle |
