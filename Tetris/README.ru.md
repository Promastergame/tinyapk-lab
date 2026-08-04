<div align="center">

# Tetris

**Компактный Android Tetris на чистом Java размером в килобайты.**

Чистый Java · SurfaceView & Canvas · Без Gradle · Без AndroidX · 16.8 КБ

[English](README.md) · [**Русский**](README.ru.md) · [Главный README](../README.ru.md)

<br>

<img src="../.github/assets/badges/java.svg" alt="Java 8">  <img src="../.github/assets/badges/android.svg" alt="Android">  <img src="../.github/assets/badges/no-gradle.svg" alt="Без Gradle">  <img src="../.github/assets/badges/apk-size.svg" alt="Размер APK">  <img src="../.github/assets/badges/license.svg" alt="MIT License">

<br><br>

<img src="../docs/images/tetris-shot-v2.jpg" alt="Скриншот Tetris" width="320">

</div>

---

> [!NOTE]
> **Внутри нет проекта Gradle.** Компилируется вручную: `aapt2 -> ecj -> R8 -> zipalign -> apksigner`.

**Tetris** — минималистичная, быстрая однофайловая Android-игра, написанная с нуля на сырых API Android SDK (`SurfaceView` и `Canvas`).

---

## <img src="../.github/assets/icons/bolt.svg" width="20"> Что нового (Обновление 04.08.2026)

- <img src="../.github/assets/icons/bolt.svg" width="14"> **Однофайловая ультра-компактная архитектура (`TetrisUltra.java`)**: Вся логика игры, рендеринг и состояния объединены в единый файл для минимизации размера APK.
- <img src="../.github/assets/icons/gamepad.svg" width="14"> **Плавные сенсорные жесты**: Плавное непрерывное перемещение деталек влево/вправо/вниз (`drag/swipe`), быстрый свайп вниз для мгновенного сброса, тап для поворота.
- <img src="../.github/assets/icons/terminal.svg" width="14"> **Интерактивная пауза**: Кнопка `[PAUSE]` / `[RESUME]` на экране и полная интеграция жизненного цикла Activity (`onPause()` / `onResume()`).
- <img src="../.github/assets/icons/flask.svg" width="14"> **Аудио-движок без выделения памяти**: Постоянное создание `AudioTrack` заменено на статичные звуковые буферы (ноль выделений памяти во время игры).
- <img src="../.github/assets/icons/layers.svg" width="14"> **Адаптивное центрирование**: Игровое поле и панель автоматически центрируются на любых экранах (планшеты, портретные и альбомные режимы).

---

## <img src="../.github/assets/icons/package.svg" width="20"> Артефакты сборки

| Артефакт | Размер | Описание |
| :--- | :--- | :--- |
| `build/LowBlocks-release.apk` | **16 848 Б** | Подписанная release-сборка после R8 |
| `build/LowBlocks.apk` | **16 848 Б** | Готовый к установке APK |

---

## <img src="../.github/assets/icons/gamepad.svg" width="20"> Особенности

- Все 7 классических тетромино (`I`, `J`, `L`, `O`, `S`, `T`, `Z`).
- Призрачная проекция приземления (ghost piece).
- Логика поворачивания у стен (wall kick).
- Очки, счётчик линий, уровни и сохранение лучшего результата (`SharedPreferences`).
- Увеличение скорости с ростом уровня.
- Панель с превью следующей фигуры.
- Эффект взрыва частиц при сгорании линий.

---

## <img src="../.github/assets/icons/terminal.svg" width="20"> Управление

| Жест / Касание | Действие |
| :--- | :--- |
| **Тап** | Повернуть фигуру |
| **Перетаскивание влево / вправо** | Движение по горизонтали |
| **Перетаскивание вниз** | Мягкий сброс (soft drop) |
| **Быстрый свайп вниз** | Мгновенный сброс (hard drop) |
| **Тап по кнопке Паузы** | Пауза / Продолжить |
| **Тап (на экране Game Over)** | Перезапуск игры |

---

## <img src="../.github/assets/icons/book.svg" width="20"> Инструкция по сборке

Для сборки проекта на Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

- [Гайд по ручной сборке](../manual-build/README.ru.md)
- [Инструкция по R8 / ProGuard](../PROGUARD_README.ru.md)

---

<div align="center">

<sub>Лицензия MIT · Подробности в <a href="../LICENSE.ru.md">LICENSE.ru.md</a></sub>

</div>
