<div align="center">

# Гайд по оптимизации с помощью R8 и ProGuard

**Уменьшение размера, обфускация и оптимизация Android APK без Android Studio и Gradle.**

[English](PROGUARD_README.md) · [Русский](PROGUARD_README.ru.md) · [Главный README](README.ru.md) · [Цепочка сборки](manual-build/README.md)

</div>

---

## <img src=".github/assets/icons/bolt.svg" width="20"> 1. Архитектура и ключевая идея

Современный оптимизатор и обфускатор от Google — **R8** — полностью встроен в `d8.jar`. Вам не требуется скачивать ProGuard или сторонние утилиты. Один и тот же JAR-файл умеет выполнять как стандартную компиляцию (D8), так и полноценную release-оптимизацию (R8).

```bash
# Debug-сборка: Java .class -> Android DEX (без сжатия)
java -cp d8.jar com.android.tools.r8.D8 ...

# Release-сборка: Сжатие + Обфускация + DEX
java -cp d8.jar com.android.tools.r8.R8 ...
```

> [!NOTE]
> R8 выполняет удаление неиспользуемого кода, инлайнинг методов, оптимизацию иерархии классов и сокращение идентификаторов за один проход.

---

## <img src=".github/assets/icons/terminal.svg" width="20"> 2. Минимальный `proguard.pro`

При использовании R8 без Gradle необходимо передать файл правил ProGuard. Зафиксируйте основные точки входа Android (такие как `Activity` и callbacks `SurfaceView`), а оставшуюся часть кода доверьте оптимизатору R8.

```proguard
# Сохраняем точки входа приложения
-keep public class * extends android.app.Activity
-keep public class * extends android.view.SurfaceView

# Сохраняем методы жизненного цикла
-keepclassmembers class * extends android.app.Activity {
    protected void onCreate(android.os.Bundle);
    protected void onDestroy();
}

# Сохраняем callbacks SurfaceView для отрисовки холста
-keepclassmembers class * extends android.view.SurfaceView {
    public void surfaceCreated(android.view.SurfaceHolder);
    public void surfaceDestroyed(android.view.SurfaceHolder);
    public void surfaceChanged(android.view.SurfaceHolder, int, int, int);
    public boolean onTouchEvent(android.view.MotionEvent);
}

# Настройки оптимизации
-dontwarn **
-allowaccessmodification
-dontpreverify
```

---

## <img src=".github/assets/icons/layers.svg" width="20"> 3. Замена D8 на R8 в цепочке сборки

В релизной сборке вызов `D8` заменяется на `R8` с передачей конфигурации ProGuard (`--pg-conf`) и файла карты переименований (`--pg-map-output`).

### Обычный шаг D8 (Debug):
```bash
java -cp d8.jar com.android.tools.r8.D8 \
  --output build/dex \
  --lib android.jar \
  $(find build/classes -name "*.class")
```

### Релизный шаг R8 (Release):
```bash
java -cp d8.jar com.android.tools.r8.R8 \
  --release \
  --min-api 26 \
  --lib android.jar \
  --output build/dex \
  --pg-conf proguard.pro \
  --pg-map-output build/mapping.txt \
  $(find build/classes -name "*.class")
```

---

## <img src=".github/assets/icons/package.svg" width="20"> 4. Упаковка и завершение сборки

После получения оптимизированного `classes.dex` оставшаяся часть ручной сборки APK остаётся неизменной: копирование `resources.apk`, внедрение `classes.dex`, выравнивание `zipalign` и подпись `apksigner`.

```bash
# 1. Подготовка базового пакета с ресурсами
cp build/apk/resources.apk build/apk_final/app-unsigned.apk

# 2. Внедрение DEX-файла после R8 в zip-архив
cd build/dex && zip -u ../apk_final/app-unsigned.apk classes.dex && cd ../..

# 3. Выравнивание zip-структуры по 4-байтовым границам
zipalign -f 4 build/apk_final/app-aligned.apk build/apk_final/app-aligned.apk

# 4. Подпись готового APK
java -jar apksigner.jar sign --ks debug.keystore --ks-pass pass:android --out MyApp-release.apk build/apk_final/app-aligned.apk
```

---

## <img src=".github/assets/icons/scale.svg" width="20"> 5. Измеренные результаты сжатия

Реальные показатели уменьшения размера DEX-файлов в проектах репозитория:

| Проект | Исходный DEX | Оптимизированный DEX | Сокращение | Ключевой фактор |
| :--- | :--- | :--- | :--- | :--- |
| **Tetris** | 15 КБ | 11 КБ | **-27%** | Инлайнинг игрового цикла и удаление неиспользуемых методов |
| **Sandbox** | 17 КБ | 13 КБ | **-24%** | Удаление мертвого кода физики частиц |
| **Типичное приложение** | 5.0 МБ | 2.8 МБ | **-44%** | Удаление неиспользуемых фреймворк-классов |

---

## <img src=".github/assets/icons/book.svg" width="20"> 6. Деобфускация stack trace с помощью `mapping.txt`

R8 активно переименовывает классы и методы в короткие символы `a`, `b`, `c`. В случае крэша в продакшене зашифрованный stack trace деобфусцируется встроенной утилитой `Retrace` с помощью сохранённого `mapping.txt`.

```bash
# Восстановление читаемого stack trace
java -cp d8.jar com.android.tools.r8.retrace.Retrace build/mapping.txt stacktrace.txt
```

> [!TIP]
> Всегда сохраняйте `mapping.txt` вместе с каждым релизным APK-файлом.

---

## <img src=".github/assets/icons/package.svg" width="20"> 7. Генерация релизного Keystore

Для публикации (например, в Google Play или альтернативных сторах) сгенерируйте 2048-битный RSA release keystore:

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias releasekey \
  -keyalg RSA -keysize 2048 -validity 25000 \
  -storepass YOUR_PASSWORD -keypass YOUR_PASSWORD \
  -dname "CN=Your Name, O=Your Company, C=US"
```

> [!WARNING]
> Сохраните `release.keystore` и пароли к нему в надёжном месте. Без него вы не сможете выпускать обновления для вашего приложения.

---

<div align="center">

**TinyAPK Lab** · Настоящие Android-приложения в масштабе килобайт

<sub>[PROGUARD_README.md](PROGUARD_README.md) · [Главный README](README.ru.md)</sub>

</div>
