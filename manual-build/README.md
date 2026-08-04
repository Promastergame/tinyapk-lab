# Manual Android Build Guides / Гайды по ручной сборке Android

This folder collects the documentation behind the manual APK workflow used in this repository.

В этой папке собрана документация по ручному процессу сборки APK, который используется в этом репозитории.

## What Is Inside / Что внутри

- [`SKILL.md`](./SKILL.md) - end-to-end notes for building an Android APK without Gradle or Android Studio.
- [`tetris/SKILL.md`](./tetris/SKILL.md) - project-oriented notes for building a minimal Java game APK similar to Tetris.
- [`proguard/SKILL.md`](./proguard/SKILL.md) - release build notes for shrinking and obfuscating with R8.

- [`SKILL.md`](./SKILL.md) - сквозные заметки по сборке Android APK без Gradle и Android Studio.
- [`tetris/SKILL.md`](./tetris/SKILL.md) - практические заметки по сборке минимальной Java-игры по типу Tetris.
- [`proguard/SKILL.md`](./proguard/SKILL.md) - заметки по release-сборке, уменьшению и обфускации через R8.

## Shared Toolchain / Общая toolchain-цепочка

```text
aapt2 -> ecj -> d8 or R8 -> zipalign -> apksigner
```

## Why It Matters / Почему это важно

The documents in this folder describe a practical alternative to the usual Gradle-heavy Android setup. They are intended for tiny apps, constrained environments, offline-oriented workflows, and developers who want full visibility into the APK build process.

Документы в этой папке описывают практичную альтернативу привычной тяжёлой Android-сборке через Gradle. Они рассчитаны на маленькие приложения, ограниченные окружения, офлайн-сценарии и разработчиков, которым важна полная прозрачность APK-сборки.
