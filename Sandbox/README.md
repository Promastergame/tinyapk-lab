<div align="center">

# Sandbox

**Minimal falling-sand sandbox with particles, growth, and cooking physics.**

Pure Java · SurfaceView & Canvas · No Gradle · No AndroidX · 16.8 KB

[**English**](README.md) · [Русский](README.ru.md) · [Root README](../README.md)

<br>

<img src="../.github/assets/badges/java.svg" alt="Java 8">  <img src="../.github/assets/badges/android.svg" alt="Android">  <img src="../.github/assets/badges/no-gradle.svg" alt="No Gradle">  <img src="../.github/assets/badges/apk-size.svg" alt="APK Size">  <img src="../.github/assets/badges/license.svg" alt="MIT License">

<br><br>

<img src="../docs/images/sandbox_en.jpg" alt="Sandbox screenshot" width="320">

</div>

---

> [!NOTE]
> **No Gradle project inside.** Compiled manually with `aapt2 -> ecj -> R8 -> zipalign -> apksigner`.

**Sandbox** is an interactive falling-sand simulation featuring powder physics, liquid flow, heat transfer, seed germination, and cooking reactions.

---

## <img src="../.github/assets/icons/bolt.svg" width="20"> What's New (Update 04.08.2026)

- <img src="../.github/assets/icons/link.svg" width="14"> **Bilingual Support (RU / EN)**: Native English localization with automatic system locale detection and an interactive `RU / EN` toggle button in the control toolbar.
- <img src="../.github/assets/icons/book.svg" width="14"> **Dynamic Text Fitting**: Automatic font scaling for button labels (`paint.setTextSize(...)` with `measureText`) guaranteeing zero text overlap or clipping on any screen width.
- <img src="../.github/assets/icons/flask.svg" width="14"> **Clean Slate Startup & Total Clear**: Removed initial demo scene so the game starts directly on a 100% blank canvas upon launch, and tapping `Clear` instantly resets the world to clean slate.
- <img src="../.github/assets/icons/image.svg" width="14"> **Adaptive Launcher Icon**: Custom vector launcher icon with dark neon background and multi-element pixel sand hourglass foreground.
- <img src="../.github/assets/icons/terminal.svg" width="14"> **Shared Windows Build Toolchain**: Fully integrated with the root `tools/windows/` build environment.

---

## <img src="../.github/assets/icons/package.svg" width="20"> Build Output

| Artifact | Size | Description |
| :--- | :--- | :--- |
| `build/Sandbox-release.apk` | **16,845 B** | Signed, R8-shrunk release build |
| `build/Sandbox.apk` | **16,845 B** | Ready-to-install release build |

---

## <img src="../.github/assets/icons/layers.svg" width="20"> Core Systems

| System | Description |
| :--- | :--- |
| **Powders** | Sand, flour, salt, yeast, and ash fall, pile up, and react with liquids or heat. |
| **Fluids** | Water flows, spreads horizontally, fills gaps, and evaporates into steam. |
| **Heat** | Fire, heat cells, stoves, and ovens drive chemical transformations across nearby materials. |
| **Growth** | Wheat and potato seeds germinate and grow when placed on suitable soil near water. |
| **Cooking** | Dough bakes into bread, potatoes fry, and ingredient combinations unlock recipes. |

---

## <img src="../.github/assets/icons/flask.svg" width="20"> Recipes

| Recipe | Result |
| :--- | :--- |
| **Flour + Water** | Dough |
| **Dough + Yeast** | Yeast dough |
| **Dough + Oven** | Bread |
| **Yeast dough + Oven** | Puffy bread |
| **Potato + Stove / Pan** | Fried potato |
| **Potato + Salt + Stove** | Tasty potato |
| **Wheat + Heat** | Flour |

---

## <img src="../.github/assets/icons/terminal.svg" width="20"> Controls

- **Touch & Drag**: Paint selected material onto the canvas.
- **`-` and `+`**: Adjust brush radius.
- **`Clear`**: Reset the simulation world.
- **`RU / EN`**: Toggle UI language live.

---

## <img src="../.github/assets/icons/book.svg" width="20"> Build Instructions

To build the project on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

- [Manual build guide](../manual-build/README.md)
- [R8 / ProGuard shrinking guide](../PROGUARD_README.md)

---

<div align="center">

<sub>MIT License · See <a href="../LICENSE">LICENSE</a> for details</sub>

</div>
