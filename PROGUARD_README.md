# R8 / ProGuard Without Gradle

Professional notes for shrinking, obfuscating, and minifying Android APKs without Android Studio or Gradle.

Профессиональные заметки о том, как уменьшать, обфусцировать и минифицировать Android APK без Android Studio и Gradle.

## Core Idea / Главная идея

R8, Google's modern replacement for ProGuard, is already bundled inside `d8.jar`. You do not need a separate download: the same jar can run plain D8 mode or full R8 release mode.

R8, современная замена ProGuard от Google, уже встроен в `d8.jar`. Отдельно скачивать его не нужно: один и тот же jar может работать как в обычном режиме D8, так и в полноценном release-режиме R8.

```bash
# Debug-style build: class -> dex, no shrinking
java -cp d8.jar com.android.tools.r8.D8 ...

# Release-style build: shrink + obfuscate + dex
java -cp d8.jar com.android.tools.r8.R8 ...
```

## Minimal `proguard.pro` / Минимальный `proguard.pro`

Use keep rules for Android entry points and SurfaceView callbacks, then allow R8 to optimize the rest.

Используйте keep-правила для Android entry points и callback-методов SurfaceView, а остальное доверьте оптимизации R8.

```proguard
-keep public class * extends android.app.Activity
-keep public class * extends android.view.SurfaceView
-keepclassmembers class * extends android.app.Activity {
    protected void onCreate(android.os.Bundle);
    protected void onDestroy();
}
-keepclassmembers class * extends android.view.SurfaceView {
    public void surfaceCreated(android.view.SurfaceHolder);
    public void surfaceDestroyed(android.view.SurfaceHolder);
    public void surfaceChanged(android.view.SurfaceHolder, int, int, int);
    public boolean onTouchEvent(android.view.MotionEvent);
}
-dontwarn **
-allowaccessmodification
-dontpreverify
```

## Replace the D8 Step / Замена шага D8

The only build-stage difference is the compiler entry point: switch from `D8` to `R8`, then provide the config and mapping output.

На уровне сборки меняется только entry point компилятора: вместо `D8` используйте `R8`, после чего передайте конфиг и путь для `mapping.txt`.

```bash
# Before: regular D8 step
java -cp d8.jar com.android.tools.r8.D8 \
  --output build/dex \
  --lib android.jar \
  $(find build/classes -name "*.class")

# After: release R8 step
java -cp d8.jar com.android.tools.r8.R8 \
  --release \
  --min-api 26 \
  --lib android.jar \
  --output build/dex \
  --pg-conf proguard.pro \
  --pg-map-output build/mapping.txt \
  $(find build/classes -name "*.class")
```

## Packaging Stays the Same / Остальная упаковка не меняется

Once `classes.dex` is produced, the rest of the APK flow is unchanged: copy `resources.apk`, inject dex, align, and sign.

После получения `classes.dex` остальная APK-цепочка не меняется: копируете `resources.apk`, добавляете dex, выравниваете архив и подписываете его.

```bash
cp build/apk/resources.apk build/apk_final/app-unsigned.apk
cd build/dex && zip -u build/apk_final/app-unsigned.apk classes.dex
zipalign -f 4 build/apk_final/app-unsigned.apk build/apk_final/app-aligned.apk
java -jar apksigner.jar sign --ks debug.keystore ... --out MyApp-release.apk app-aligned.apk
```

## Measured Results / Практические результаты

| Project | Before | After | Reduction |
| --- | --- | --- | --- |
| Tetris | 15 KB DEX | 11 KB DEX | -27% |
| Sandbox | 17 KB DEX | 13 KB DEX | -24% |
| Typical app | 5 MB | ~2.8 MB | ~44% |

## Keep `mapping.txt` / Сохраняйте `mapping.txt`

R8 aggressively renames symbols to short identifiers such as `a`, `b`, and `c`. If the app crashes, the stack trace becomes difficult to read unless you keep the generated mapping file.

R8 активно переименовывает символы в короткие идентификаторы вроде `a`, `b` и `c`. Если приложение упадёт, stack trace будет плохо читаться, если вы не сохраните сгенерированный `mapping.txt`.

```bash
java -cp d8.jar com.android.tools.r8.retrace.Retrace mapping.txt stacktrace.txt
```

## Release Keystore / Release keystore

Use a dedicated release keystore for production distribution, especially if the APK is intended for Google Play or another update channel.

Для production-распространения используйте отдельный release keystore, особенно если APK планируется публиковать в Google Play или другом канале обновлений.

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias releasekey \
  -keyalg RSA -keysize 2048 -validity 25000 \
  -storepass YOUR_PASSWORD -keypass YOUR_PASSWORD \
  -dname "CN=Your Name, O=Your Company, C=US"
```

Never lose `release.keystore`: without it, you cannot ship updates to the same application identity.

Никогда не теряйте `release.keystore`: без него нельзя выпускать обновления под той же идентичностью приложения.
