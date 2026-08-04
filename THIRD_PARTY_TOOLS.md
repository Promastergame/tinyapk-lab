<div align="center">

# Third-Party Tools Notice

**Notice regarding Android SDK, platform tooling, and third-party software licensing.**

[English](THIRD_PARTY_TOOLS.md) · [Русский](THIRD_PARTY_TOOLS.ru.md) · [Main README](README.md) · [License](LICENSE)

</div>

---

## <img src=".github/assets/icons/scale.svg" width="20"> 1. Scope & Original Code Licensing

The MIT License specified in the [LICENSE](LICENSE) file applies strictly to the original source code, documentation, scripts, and assets authored directly for this repository (unless a file explicitly specifies a different license).

> [!NOTE]
> All original materials created by the repository author are licensed under the permissive **MIT License**.

---

## <img src=".github/assets/icons/terminal.svg" width="20"> 2. Permitted Use with Android Tooling

You are expressly permitted to use this repository alongside Android platform and build tools to assemble, test, package, sign, and distribute derivative works.

Supported toolchains and utilities include (but are not limited to):

| Tool / Component | Purpose & Role in Build Chain |
| :--- | :--- |
| `android.jar` | Android framework API definitions |
| `aapt2` | Android Asset Packaging Tool (compiling resources & packaging APK) |
| `ecj` | Eclipse Compiler for Java (stand-alone Java compilation) |
| `d8` / `R8` | Java bytecode to Dex compiler & ProGuard shrinker/optimizer |
| `zipalign` | Zip alignment tool for APK execution performance |
| `apksigner` | APK signing and signature verification tool |
| `adb` & `keytool` | Android Debug Bridge & Java Keystore credential management |

---

## <img src=".github/assets/icons/package.svg" width="20"> 3. No Re-Licensing of Upstream Tools

Nothing contained in this repository re-licenses, transfers, modifies, or overrides the licenses, terms of service, trademarks, or distribution rules established by upstream vendor projects, including:

- **Google & Android Open Source Project (AOSP)**
- **Eclipse Foundation**
- **Oracle Corporation**
- Any other third-party vendor, contributor, or project.

> [!IMPORTANT]
> Third-party binaries, libraries, SDKs, and build utilities retain their respective upstream licenses.

---

## <img src=".github/assets/icons/bolt.svg" width="20"> 4. User Responsibility

If you download, bundle, redistribute, or depend on third-party build tools, SDK components, binaries, or external assets:

- **Compliance**: You are solely responsible for reviewing and complying with the respective licenses and terms of service governing those components.
- **Redistribution**: Ensure your usage and distribution of third-party tools adhere to vendor redistribution permissions.

---

## <img src=".github/assets/icons/book.svg" width="20"> 5. Practical Summary

| Aspect / Question | Summary Policy |
| :--- | :--- |
| **Repository Code** | Free to use, modify, and distribute under the **MIT License**. |
| **Building & Shipping** | Fully allowed to build and ship applications using Android SDK tooling. |
| **Tooling Licensing** | Android tools remain governed by their **original upstream licenses**. |

---

<div align="center">

**TinyAPK Lab** · Kilobyte-scale Android engineering

<sub>[LICENSE](LICENSE) · [THIRD_PARTY_TOOLS.ru.md](THIRD_PARTY_TOOLS.ru.md)</sub>

</div>
