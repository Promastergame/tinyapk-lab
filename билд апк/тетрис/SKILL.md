---
name: build-android-game-apk
description: Build a minimal Android game APK (like Tetris) from scratch using only Java, Canvas, and android.jar — no Gradle, no Kotlin, no libraries. Result: 16KB APK.
version: 1.0
author: Promaster
---

# SKILL: Build Android Game APK From Scratch

## Philosophy
The smallest possible Android APK uses ZERO third-party libraries. Everything needed is already on the device inside `android.jar`. This skill produces a working game in ~16KB — compared to 5-15MB with Gradle + Kotlin + AndroidX.

**Stack:**
- Pure Java (no Kotlin — saves ~1MB Kotlin runtime)
- `android.Canvas` + `SurfaceView` for rendering (no game engine needed)
- `MotionEvent` for touch/swipe input
- `android.jar` only — no androidx, no dependencies
- Built manually: `aapt2 → ecj → d8 → zip → zipalign → apksigner`

---

## Project structure (minimal)

```
MyGame/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/mygame/
│   │   ├── game/
│   │   │   └── GameLogic.java     ← pure game state, no Android imports
│   │   └── ui/
│   │       ├── GameView.java      ← SurfaceView + Canvas + touch
│   │       └── MainActivity.java  ← fullscreen setup, setContentView
│   └── res/values/
│       └── strings.xml            ← just app_name
└── AndroidManifest.xml            ← package, minSdk, activity
```

**Key principle: separate game logic from rendering.** `GameLogic.java` has zero Android imports — just plain Java. `GameView.java` handles all rendering and input.

---

## Step 1: Write the game logic (pure Java)

`GameLogic.java` — no Android imports at all:

```java
package com.mygame.game;

import java.util.Random;

public class GameLogic {
    public static final int COLS = 10;
    public static final int ROWS = 20;

    // Game state fields
    public int[][] board;
    public int score = 0;
    public boolean gameOver = false;

    public GameLogic() {
        board = new int[ROWS][COLS];
        // initialize game...
    }

    // All game methods: move, rotate, collision, scoring
    public boolean moveLeft()  { /* ... */ return true; }
    public boolean moveRight() { /* ... */ return true; }
    public boolean moveDown()  { /* ... */ return true; }
    public void hardDrop()     { /* ... */ }
    public void rotate()       { /* ... */ }
}
```

**Why separate?** Pure Java compiles faster, is easier to test, and has no Android dependency issues.

---

## Step 2: Write the SurfaceView renderer

`GameView.java` — extends SurfaceView, runs its own game loop thread:

```java
package com.mygame.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.mygame.game.GameLogic;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameLogic game;
    private GameThread thread;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Swipe detection fields
    private float touchStartX, touchStartY;
    private long touchStartTime;
    private static final float SWIPE_THRESHOLD = 60f;
    private static final long TAP_MAX_MS = 200;

    public GameView(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);
        game = new GameLogic();
    }

    // ── Game Loop Thread ─────────────────────────────────
    private class GameThread extends Thread {
        private volatile boolean running = true;
        private long lastTick = System.currentTimeMillis();

        @Override public void run() {
            while (running) {
                long now = System.currentTimeMillis();
                if (!game.gameOver && now - lastTick > 800) {
                    game.moveDown();
                    lastTick = now;
                }
                drawFrame();
                try { Thread.sleep(16); } catch (InterruptedException ignored) {}
            }
        }

        void stopLoop() { running = false; }
    }

    // ── Rendering ────────────────────────────────────────
    private void drawFrame() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            // Draw dark gradient background
            paint.setShader(new LinearGradient(0, 0, 0, canvas.getHeight(),
                    0xFF0D0D1A, 0xFF1A1A2E, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
            paint.setShader(null);

            // Draw game elements...
            drawBoard(canvas);
            drawCurrentPiece(canvas);

        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    // ── Drawing helpers ──────────────────────────────────
    private void drawBoard(Canvas canvas) { /* draw grid + locked cells */ }
    private void drawCurrentPiece(Canvas canvas) { /* draw active piece */ }

    // Draw a single cell with highlight effect:
    private void drawCell(Canvas canvas, float x, float y, float size, int color) {
        float pad = size * 0.05f;
        RectF rect = new RectF(x+pad, y+pad, x+size-pad, y+size-pad);

        // Main fill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(rect, 4, 4, paint);

        // Top-left highlight (gives 3D look)
        paint.setColor(0x66FFFFFF);
        canvas.drawRoundRect(new RectF(x+pad, y+pad, x+size-pad, y+pad+size*0.25f), 4, 2, paint);

        // Dark border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(darken(color));
        canvas.drawRoundRect(rect, 4, 4, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private int darken(int color) {
        int r = (int)(((color>>16)&0xFF)*0.6f);
        int g = (int)(((color>>8)&0xFF)*0.6f);
        int b = (int)((color&0xFF)*0.6f);
        return 0xFF000000 | (r<<16) | (g<<8) | b;
    }

    // ── Swipe / Touch ────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = e.getX();
                touchStartY = e.getY();
                touchStartTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                if (game.gameOver) { game = new GameLogic(); return true; }
                float dx = e.getX() - touchStartX;
                float dy = e.getY() - touchStartY;
                long dt = System.currentTimeMillis() - touchStartTime;

                if (Math.abs(dx) < 20 && Math.abs(dy) < 20 && dt < TAP_MAX_MS) {
                    game.rotate();                         // tap = rotate
                } else if (Math.abs(dx) > Math.abs(dy)) {
                    if (Math.abs(dx) > SWIPE_THRESHOLD)
                        if (dx > 0) game.moveRight();
                        else game.moveLeft();              // swipe left/right = move
                } else {
                    if (dy > SWIPE_THRESHOLD) game.hardDrop();  // swipe down = hard drop
                    else if (dy < -SWIPE_THRESHOLD) game.rotate(); // swipe up = rotate
                }
                break;
        }
        return true;
    }

    // ── Lifecycle ────────────────────────────────────────
    @Override public void surfaceCreated(SurfaceHolder h) {
        thread = new GameThread(); thread.start();
    }
    @Override public void surfaceDestroyed(SurfaceHolder h) {
        thread.stopLoop();
        try { thread.join(); } catch (InterruptedException ignored) {}
    }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
}
```

---

## Step 3: Write MainActivity (minimal)

```java
package com.mygame.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen setup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Hide system UI (immersive)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // GameView IS the entire content — no XML layout needed!
        setContentView(new GameView(this));
    }
}
```

**No XML layout file needed!** `setContentView(new GameView(this))` directly sets the SurfaceView.

---

## Step 4: AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.mygame">

    <uses-sdk
        android:minSdkVersion="26"
        android:targetSdkVersion="34"/>

    <application
        android:label="My Game"
        android:allowBackup="false"
        android:theme="@android:style/Theme.Black.NoTitleBar.Fullscreen">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

    </application>
</manifest>
```

**Critical attributes:**
- `uses-sdk` with both min and target — prevents "designed for older Android" warning
- `screenOrientation="portrait"` — prevents game loop from restarting on rotation
- `configChanges="orientation|screenSize"` — handles rotation without Activity restart
- `theme="@android:style/Theme.Black.NoTitleBar.Fullscreen"` — built-in theme, no androidx needed

---

## Step 5: res/values/strings.xml (minimal)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">My Game</string>
</resources>
```

This is the only resource file needed. No colors.xml, no themes.xml, no layout XMLs.

---

## Step 6: Build commands (all 6 steps)

```bash
AAPT2=/home/claude/termux-aapt/prebuilt-binary/x86-64/aapt2
ANDROID_JAR=/home/claude/sdk/platforms/android-34/android.jar
PROJ=/home/claude/MyGame/app/src/main
BUILD=/home/claude/build_game

# Create build dirs
mkdir -p $BUILD/res $BUILD/classes $BUILD/dex $BUILD/gen $BUILD/apk $BUILD/apk_final

# 1. Compile resources
$AAPT2 compile --dir $PROJ/res -o $BUILD/res/compiled.zip

# 2. Link resources → resources.apk + R.java
$AAPT2 link \
  -o $BUILD/apk/resources.apk \
  -I $ANDROID_JAR \
  --manifest $PROJ/AndroidManifest.xml \
  --java $BUILD/gen \
  --auto-add-overlay \
  $BUILD/res/compiled.zip

# 3. Compile Java — TWO PASSES (game logic first, then UI which depends on it)
# Pass 1: game logic + R.java
java -jar /home/claude/ecj.jar -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath $ANDROID_JAR \
  -classpath $ANDROID_JAR \
  -d $BUILD/classes \
  $PROJ/java/com/mygame/game/GameLogic.java \
  $BUILD/gen/com/mygame/R.java

# Pass 2: UI files (need game classes + R in classpath)
java -jar /home/claude/ecj.jar -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath $ANDROID_JAR \
  -classpath "$ANDROID_JAR:$BUILD/classes" \
  -d $BUILD/classes \
  $PROJ/java/com/mygame/ui/GameView.java \
  $PROJ/java/com/mygame/ui/MainActivity.java

# 4. Convert .class → DEX
java -cp /home/claude/sdk/build-tools/34.0.0/lib/d8.jar com.android.tools.r8.D8 \
  --output $BUILD/dex \
  --lib $ANDROID_JAR \
  $(find $BUILD/classes -name "*.class")

# 5. Package APK
cp $BUILD/apk/resources.apk $BUILD/apk_final/app-unsigned.apk
cd $BUILD/dex && zip -u $BUILD/apk_final/app-unsigned.apk classes.dex

# 6. Align + Sign
zipalign -f 4 $BUILD/apk_final/app-unsigned.apk $BUILD/apk_final/app-aligned.apk

java -jar /home/claude/sdk/build-tools/34.0.0/lib/apksigner.jar sign \
  --ks /home/claude/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out $BUILD/apk_final/MyGame.apk \
  $BUILD/apk_final/app-aligned.apk
```

---

## Tetris-specific: shapes, colors, ghost piece, wall kick

```java
// 7 tetromino shapes
private static final int[][][] SHAPES = {
    {{1,1,1,1}},           // I
    {{1,1},{1,1}},          // O
    {{0,1,0},{1,1,1}},      // T
    {{1,0},{1,0},{1,1}},    // L
    {{0,1},{0,1},{1,1}},    // J
    {{0,1,1},{1,1,0}},      // S
    {{1,1,0},{0,1,1}}       // Z
};

// Vibrant ARGB colors
public static final int[] COLORS = {
    0xFF00F5FF, // I cyan
    0xFFFFE000, // O yellow
    0xFFBF00FF, // T purple
    0xFFFF8C00, // L orange
    0xFF0050FF, // J blue
    0xFF00E060, // S green
    0xFF FF1744, // Z red
};

// Ghost piece — show where piece will land
public int ghostY() {
    int gy = currentY;
    while (fits(current, currentX, gy+1)) gy++;
    return gy;
}

// Wall kick — try shifting ±1, ±2 on rotation
public void rotate() {
    int[][] rotated = rotateCW(current);
    int[] kicks = {0, -1, 1, -2, 2};
    for (int kick : kicks) {
        if (fits(rotated, currentX + kick, currentY)) {
            current = rotated;
            currentX += kick;
            return;
        }
    }
}

// Clockwise rotation
private int[][] rotateCW(int[][] s) {
    int rows = s.length, cols = s[0].length;
    int[][] r = new int[cols][rows];
    for (int i = 0; i < rows; i++)
        for (int j = 0; j < cols; j++)
            r[j][rows-1-i] = s[i][j];
    return r;
}

// Scoring (classic Tetris)
private int scoreForLines(int n) {
    switch (n) {
        case 1: return 100;
        case 2: return 300;
        case 3: return 500;
        case 4: return 800; // Tetris!
        default: return 0;
    }
}
```

---

## Layout math for Canvas rendering

```java
// Calculate cell size to fit board on screen with panel on right
cellSize = Math.min(
    (height * 0.95f) / ROWS,
    (width * 0.6f) / COLS
);

float boardLeft = width * 0.05f;
float boardTop  = (height - cellSize * ROWS) / 2f;
float panelX    = boardLeft + cellSize * COLS + cellSize * 0.8f;

// Convert grid coords to screen coords
float screenX = boardLeft + col * cellSize;
float screenY = boardTop  + row * cellSize;
```

---

## Common pitfalls and fixes

| Problem | Fix |
|---------|-----|
| `mkdir -p a/{b,c}` creates literal `{b,c}` folder | Use separate `mkdir -p a/b && mkdir -p a/c` commands |
| ECJ `Cannot invoke Path.getFileSystem()` | Don't use `@file` syntax — pass files directly as arguments |
| `R cannot be resolved` in pass 2 | Add `$BUILD/classes` to `-classpath` in pass 2 |
| `LambdaMetafactory cannot be resolved` | Use `-source 8 -target 8` NOT `--release 8` for two-pass compile |
| Game loop blocks UI thread | Always run game loop in a separate `Thread`, never on main thread |
| Screen rotates and restarts game | Add `android:configChanges="orientation|screenSize"` to activity |
| Screen goes dark during gameplay | Add `FLAG_KEEP_SCREEN_ON` in onCreate |
| App crashes on back press during game | Override `onDestroy` to stop the game thread |
| `surfaceDestroyed` crashes | Always call `thread.join()` after `thread.stopLoop()` |
| Ghost piece flickers | Draw ghost BEFORE current piece in render order |
| Cells look flat | Add top-left white highlight rect at 25% cell height |

---

## APK size breakdown (why 16KB)

| Component | Size |
|-----------|------|
| `classes.dex` (compiled Java) | ~15KB |
| `resources.apk` (manifest + strings) | ~1KB |
| Total signed APK | **~16KB** |

Compare to typical Gradle + Kotlin + AndroidX app: **5-15MB** (300-900x larger).

The difference: no Kotlin stdlib (~1MB), no androidx (~3MB), no Gradle metadata (~1MB), no ProGuard/R8 overhead.