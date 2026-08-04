<div align="center">

# Гайды по ручной сборке Android

**Пошаговое руководство по компиляции, упаковке, оптимизации и подписи Android APK без Gradle и Android Studio.**

[English](README.md) · [Русский](README.ru.md) · [Главный README](../README.ru.md) · [Гайд по R8](../PROGUARD_README.ru.md)

</div>

---

## <img src="../.github/assets/icons/terminal.svg" width="20"> 1. Обзор и архитектура цепочки сборки

В этой папке собрана документация и технические заметки по сборке микроскопических Android-приложений с использованием консольных инструментов Android SDK и JDK.

### Схема цепочки сборки:

```text
  [Ресурсы и XML]        ───► aapt2 compile/link ───► resources.apk ──┐
                                                                      ├──► zip -u ──► zipalign ──► apksigner ──► Готовый APK
  [Исходники Java]       ───► ecj компилятор     ───► classes.dex   ──┘
                                                       │
                                            (Опционально: R8 оптимизатор)
```

| Шаг | Инструмент | Входные данные | Выходные данные | Назначение |
| :--- | :--- | :--- | :--- | :--- |
| **1. Компиляция ресурсов** | `aapt2` | `res/`, `AndroidManifest.xml` | `resources.apk`, `R.java` | Компиляция XML-разметки, ассетов и генерация ID ресурсов |
| **2. Компиляция Java** | `ecj` / `javac` | `.java` файлы, `android.jar` | `.class` файлы | Компиляция Java-кода в байт-код JVM |
| **3. Конвертация в DEX** | `d8` / `R8` | `.class` файлы | `classes.dex` | Преобразование байт-кода JVM в байт-код Dalvik/ART (+ опциональное сжатие R8) |
| **4. Упаковка** | `zip` | `resources.apk`, `classes.dex` | `app-unsigned.apk` | Объединение ресурсов и байт-кода DEX в единый APK-пакет |
| **5. Выравнивание zip** | `zipalign` | `app-unsigned.apk` | `app-aligned.apk` | Выравнивание несжатых ассетов по 4-байтовым границам для mmap |
| **6. Подпись APK** | `apksigner` | `app-aligned.apk`, `.keystore` | `app-release.apk` | Цифровая подпись APK (схема v2/v3) для установки на устройство |

---

## <img src="../.github/assets/icons/book.svg" width="20"> 2. Содержание документации

Подробные руководства и пошаговые инструкции, включённые в эту папку:

| Документ / Гайд | Назначение и содержание |
| :--- | :--- |
| <img src="../.github/assets/icons/terminal.svg" width="16"> [**Базовый гайд по ручной сборке**](./SKILL.md) | Сквозные заметки по сборке минимального Android APK с нуля без Gradle. |
| <img src="../.github/assets/icons/gamepad.svg" width="16"> [**Сборка проекта Tetris**](./tetris/SKILL.md) | Практическое руководство по сборке Java-игры без сторонних библиотек. |
| <img src="../.github/assets/icons/package.svg" width="16"> [**Оптимизация и R8**](./proguard/SKILL.md) | Заметки по release-сборке, правилам ProGuard, обфускации и сжатию байт-кода. |

---

## <img src="../.github/assets/icons/flask.svg" width="20"> 3. Зачем нужна ручная сборка

Классическая разработка под Android опирается на Gradle, Android Gradle Plugin (AGP), библиотеки AndroidX и Android Studio — это добавляет сотни мегабайт инфраструктурных зависимостей ещё до отрисовки первого пикселя.

### Преимущества ручной цепочки сборки:

- **Мгновенная скорость**: Сборка и упаковка за доли секунды без задержек на запуск демона Gradle.
- **Размер в килобайтах**: Размер готовых APK исчисляется килобайтами (10-30 КБ) вместо мегабайт (20-50 МБ).
- **100% прозрачность**: Полная видимость каждого шага, каждого флага и вызова утилит.
- **Нуль внешних зависимостей**: Работает полностью офлайн только с помощью консольных утилит JDK и Android SDK.

---

<div align="center">

**TinyAPK Lab** · Настоящие Android-приложения в масштабе килобайт

<sub>[README.md](README.md) · [Главный README](../README.ru.md)</sub>

</div>
