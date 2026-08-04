<div align="center">

# Manual Android Build Guides

**Complete step-by-step guides for compiling, packaging, optimizing, and signing Android APKs without Gradle or Android Studio.**

[English](README.md) · [Русский](README.ru.md) · [Main README](../README.md) · [R8 Guide](../PROGUARD_README.md)

</div>

---

## <img src="../.github/assets/icons/terminal.svg" width="20"> 1. Overview & Build Chain

This directory contains reference guides and technical notes for building kilobyte-scale Android applications using direct CLI tools provided by the Android SDK and JDK.

### Core Build Pipeline Architecture:

```text
  [Android Assets & XML]  ───► aapt2 compile/link ───► resources.apk ──┐
                                                                       ├──► zip -u ──► zipalign ──► apksigner ──► Final APK
  [Java Source Files]     ───► ecj compiler       ───► classes.dex   ──┘
                                                       │
                                              (Optional: R8 Optimizer)
```

| Step | Tool | Input | Output | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **1. Resource Compilation** | `aapt2` | `res/`, `AndroidManifest.xml` | `resources.apk`, `R.java` | Compiles XML layouts, assets, and generates resource IDs |
| **2. Java Compilation** | `ecj` / `javac` | `.java` files, `android.jar` | `.class` files | Compiles Java code into JVM bytecode |
| **3. DEX Conversion** | `d8` / `R8` | `.class` files | `classes.dex` | Converts JVM bytecode to Dalvik/ART bytecode (+ optional R8 shrinking) |
| **4. Packaging** | `zip` | `resources.apk`, `classes.dex` | `app-unsigned.apk` | Bundles resources and compiled DEX bytecode into an APK package |
| **5. Zip Alignment** | `zipalign` | `app-unsigned.apk` | `app-aligned.apk` | Aligns uncompressed assets to 4-byte boundaries for memory mapping |
| **6. Code Signing** | `apksigner` | `app-aligned.apk`, `.keystore` | `app-release.apk` | Digitally signs the APK v2/v3 scheme for installation on Android |

---

## <img src="../.github/assets/icons/book.svg" width="20"> 2. Documentation Directory

Detailed walkthroughs and step-by-step guides included in this folder:

| Document / Guide | Scope & Content |
| :--- | :--- |
| <img src="../.github/assets/icons/terminal.svg" width="16"> [**Core Build Skill Guide**](./SKILL.md) | End-to-end walkthrough for building a minimal Android APK from scratch without Gradle. |
| <img src="../.github/assets/icons/gamepad.svg" width="16"> [**Tetris Build Guide**](./tetris/SKILL.md) | Hands-on guide for building a pure Java canvas game APK without third-party libraries. |
| <img src="../.github/assets/icons/package.svg" width="16"> [**R8 Optimization Guide**](./proguard/SKILL.md) | Release optimization notes, ProGuard rules, obfuscation, and byte-code shrinking. |

---

## <img src="../.github/assets/icons/flask.svg" width="20"> 3. Why Manual Building Matters

Traditional Android development relies on Gradle, Android Gradle Plugin (AGP), AndroidX libraries, and Android Studio — adding hundreds of megabytes of dependencies before rendering a single pixel.

### Benefits of the Manual Build Chain:

- **Instant Build Speed**: Sub-second compilation and packaging cycles without daemon startup overhead.
- **Kilobyte-Scale Output**: APK sizes measured in kilobytes (10-30 KB) instead of megabytes (20-50 MB).
- **100% Transparency**: Complete visibility into every single step, flag, and tool invocation in the build process.
- **Zero External Dependencies**: Works fully offline with only standard JDK and Android SDK command-line tools.

---

<div align="center">

**TinyAPK Lab** · Kilobyte-scale Android engineering

<sub>[README.ru.md](README.ru.md) · [Main README](../README.md)</sub>

</div>
