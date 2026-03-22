package com.sandbox.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.sandbox.game.Particle;
import com.sandbox.game.SandWorld;

public class SandView extends SurfaceView implements SurfaceHolder.Callback {

    private SandWorld world;
    private SimThread thread;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int WORLD_W = 120;
    private static final int WORLD_H = 160;

    private int selectedTool = Particle.SAND;
    private int brushRadius = 2;

    private Bitmap worldBitmap;
    private int[] pixels;

    // Layout
    private float scaleX, scaleY;
    private float toolbarH;
    private float rowH;     // высота одной строки инструментов
    private float ctrlH;    // высота строки управления (очистить, кисть)
    private int SW, SH;     // размер экрана

    // Touch
    private boolean touching = false;
    private float touchX, touchY;
    private boolean touchingToolbar = false;

    // Инструменты — 2 ряда по 7
    private static final int[] ROW1 = {
        Particle.SAND, Particle.WATER, Particle.DIRT, Particle.GRASS,
        Particle.FIRE, Particle.WALL, Particle.STOVE
    };
    private static final int[] ROW2 = {
        Particle.WHEAT_SEED, Particle.POTATO_SEED,
        Particle.FLOUR, Particle.SALT, Particle.YEAST,
        Particle.OVEN, Particle.EMPTY
    };

    public SandView(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);
        world = new SandWorld(WORLD_W, WORLD_H);
        pixels = new int[WORLD_W * WORLD_H];
        worldBitmap = Bitmap.createBitmap(WORLD_W, WORLD_H, Bitmap.Config.ARGB_8888);
        buildStartScene();
    }

    private void buildStartScene() {
        for (int x = 0; x < WORLD_W; x++) {
            world.set(x, WORLD_H-1, Particle.DIRT);
            world.set(x, WORLD_H-2, Particle.DIRT);
            if (x > WORLD_W/4 && x < 3*WORLD_W/4)
                world.set(x, WORLD_H-3, Particle.GRASS);
        }
        // Плита + сковородка справа
        for (int x = WORLD_W-14; x < WORLD_W-7; x++)
            world.set(x, WORLD_H-3, Particle.STOVE);
        for (int x = WORLD_W-13; x < WORLD_W-8; x++)
            world.set(x, WORLD_H-4, Particle.WALL);
        // Духовка слева
        for (int x = 4; x < 14; x++) {
            world.set(x, WORLD_H-3, Particle.OVEN);
            world.set(x, WORLD_H-4, Particle.OVEN);
            world.set(x, WORLD_H-5, Particle.OVEN);
        }
        world.set(4,  WORLD_H-4, Particle.EMPTY);
        world.set(13, WORLD_H-4, Particle.EMPTY);
    }

    // ── Sim Thread ───────────────────────────────────────
    private class SimThread extends Thread {
        volatile boolean running = true;
        @Override public void run() {
            long last = System.currentTimeMillis();
            while (running) {
                long now = System.currentTimeMillis();
                if (now - last >= 16) {
                    if (touching && !touchingToolbar) {
                        int wx = screenToWorldX(touchX);
                        int wy = screenToWorldY(touchY);
                        world.paint(wx, wy, selectedTool, brushRadius);
                    }
                    world.step();
                    render();
                    last = now;
                }
                try { Thread.sleep(4); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ── Render ───────────────────────────────────────────
    private void render() {
        for (int i = 0; i < WORLD_W * WORLD_H; i++)
            pixels[i] = Particle.COLORS[world.grid[i]];
        worldBitmap.setPixels(pixels, 0, WORLD_W, 0, 0, WORLD_W, WORLD_H);

        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try { drawAll(canvas); }
        finally { holder.unlockCanvasAndPost(canvas); }
    }

    private void drawAll(Canvas canvas) {
        SW = canvas.getWidth(); SH = canvas.getHeight();

        // Высоты зон
        ctrlH = SH * 0.07f;
        rowH  = SH * 0.11f;
        toolbarH = ctrlH + rowH * 2;
        float worldH = SH - toolbarH;

        scaleX = SW / (float)WORLD_W;
        scaleY = worldH / (float)WORLD_H;

        // Фон
        canvas.drawColor(0xFF0D0D1A);

        // Мир
        paint.setFilterBitmap(false);
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setScale(scaleX, scaleY);
        canvas.drawBitmap(worldBitmap, m, paint);

        // Курсор
        if (touching && !touchingToolbar) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(0xAAFFFFFF);
            float cx = (screenToWorldX(touchX) + 0.5f) * scaleX;
            float cy = (screenToWorldY(touchY) + 0.5f) * scaleY;
            float cr = (brushRadius + 0.5f) * Math.max(scaleX, scaleY);
            canvas.drawCircle(cx, cy, cr, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        drawToolbar(canvas);
    }

    private void drawToolbar(Canvas canvas) {
        float tbY = SH - toolbarH;

        // Фон тулбара
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF12122A);
        canvas.drawRect(0, tbY, SW, SH, paint);

        // Разделитель
        paint.setColor(0xFF2A2A50);
        canvas.drawRect(0, tbY, SW, tbY + 1.5f, paint);

        float row1Y = tbY + ctrlH;
        float row2Y = row1Y + rowH;

        // Строка 1
        drawToolRow(canvas, ROW1, row1Y, rowH);
        // Строка 2
        drawToolRow(canvas, ROW2, row2Y, rowH);

        // Строка управления
        drawControlRow(canvas, tbY, ctrlH);
    }

    private void drawToolRow(Canvas canvas, int[] tools, float rowY, float rH) {
        float btnW = SW / (float)tools.length;
        float dotR = rH * 0.30f;
        float textSize = rH * 0.20f;

        for (int i = 0; i < tools.length; i++) {
            int tool = tools[i];
            float bx = i * btnW;
            float cx = bx + btnW / 2f;
            boolean sel = (tool == selectedTool);

            // Выделение выбранного
            if (sel) {
                paint.setColor(0xFF252550);
                canvas.drawRoundRect(new RectF(bx + 3, rowY + 3, bx + btnW - 3, rowY + rH - 3), 8, 8, paint);
                // Подчёркивание
                paint.setColor(Particle.COLORS[tool] != 0 ? Particle.COLORS[tool] : 0xFF5555AA);
                canvas.drawRoundRect(new RectF(bx + 8, rowY + rH - 5, bx + btnW - 8, rowY + rH - 2), 2, 2, paint);
            }

            // Кружок
            float dotY = rowY + rH * 0.42f;
            if (tool == Particle.EMPTY) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.5f);
                paint.setColor(0xFF555577);
                canvas.drawCircle(cx, dotY, dotR, paint);
                // Крест внутри
                paint.setStrokeWidth(1.5f);
                canvas.drawLine(cx - dotR*0.6f, dotY - dotR*0.6f, cx + dotR*0.6f, dotY + dotR*0.6f, paint);
                canvas.drawLine(cx + dotR*0.6f, dotY - dotR*0.6f, cx - dotR*0.6f, dotY + dotR*0.6f, paint);
                paint.setStyle(Paint.Style.FILL);
            } else {
                int color = Particle.COLORS[tool];
                // Тень кружка
                paint.setColor(0x33000000);
                canvas.drawCircle(cx + 1, dotY + 1, dotR, paint);
                // Основной цвет
                paint.setColor(color);
                canvas.drawCircle(cx, dotY, dotR, paint);
                // Блик
                paint.setColor(0x44FFFFFF);
                canvas.drawCircle(cx - dotR*0.25f, dotY - dotR*0.25f, dotR * 0.4f, paint);
            }

            // Подпись
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(textSize);
            paint.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            paint.setColor(sel ? 0xFFFFFFFF : 0xFF7777AA);
            canvas.drawText(shortName(tool), cx, rowY + rH * 0.88f, paint);
        }
    }

    private void drawControlRow(Canvas canvas, float rowY, float rH) {
        float pad = 10f;
        float btnH = rH * 0.62f;
        float btnY = rowY + (rH - btnH) / 2f;
        float textSize = rH * 0.28f;

        // Кнопка "Очистить"
        float clearW = SW * 0.22f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF3A1A1A);
        canvas.drawRoundRect(new RectF(pad, btnY, pad + clearW, btnY + btnH), 8, 8, paint);
        paint.setColor(0xFFFF5555);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("Очистить", pad + clearW/2, btnY + btnH*0.67f, paint);

        // Размер кисти
        float brushX = SW * 0.33f;
        paint.setColor(0xFF7777AA);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(textSize * 0.85f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Кисть:", brushX, btnY + btnH*0.55f, paint);

        // Кнопки - и +
        float btnSize = btnH * 0.85f;
        float minusX = brushX + SW * 0.14f;
        float plusX  = minusX + btnSize + SW * 0.10f;
        float byY    = btnY + (btnH - btnSize) / 2f;

        // Кнопка "−"
        paint.setColor(0xFF252545);
        canvas.drawRoundRect(new RectF(minusX, byY, minusX+btnSize, byY+btnSize), 6,6, paint);
        paint.setColor(0xFFAAAADD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize * 1.2f);
        canvas.drawText("-", minusX + btnSize/2, byY + btnSize*0.72f, paint);

        // Значение
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(String.valueOf(brushRadius*2+1), minusX + btnSize + SW*0.05f, byY + btnSize*0.72f, paint);

        // Кнопка "+"
        paint.setColor(0xFF252545);
        canvas.drawRoundRect(new RectF(plusX, byY, plusX+btnSize, byY+btnSize), 6,6, paint);
        paint.setColor(0xFFAAAADD);
        paint.setTextSize(textSize * 1.2f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("+", plusX + btnSize/2, byY + btnSize*0.72f, paint);
    }

    private String shortName(int tool) {
        switch(tool) {
            case Particle.EMPTY:       return "Стер.";
            case Particle.SAND:        return "Песок";
            case Particle.WATER:       return "Вода";
            case Particle.DIRT:        return "Земля";
            case Particle.GRASS:       return "Дёрн";
            case Particle.WHEAT_SEED:  return "Пшен.";
            case Particle.POTATO_SEED: return "Карт.";
            case Particle.FLOUR:       return "Мука";
            case Particle.SALT:        return "Соль";
            case Particle.YEAST:       return "Дрожж";
            case Particle.FIRE:        return "Огонь";
            case Particle.WALL:        return "Скор.";
            case Particle.OVEN:        return "Духовка";
            case Particle.STOVE:       return "Плита";
            default: return "?";
        }
    }

    // ── Touch ────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        float x = e.getX(), y = e.getY();
        float tbY = SH - toolbarH;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (y >= tbY) {
                    touchingToolbar = true;
                    handleToolbarTouch(x, y, tbY);
                } else {
                    touchingToolbar = false;
                    touching = true;
                    touchX = x; touchY = y;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!touchingToolbar) { touchX = x; touchY = y; }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = false; touchingToolbar = false; break;
        }
        return true;
    }

    private void handleToolbarTouch(float x, float y, float tbY) {
        float row1Y = tbY + ctrlH;
        float row2Y = row1Y + rowH;

        if (y < row1Y) {
            // Строка управления
            float pad = 10f;
            float clearW = SW * 0.22f;
            if (x < pad + clearW) {
                // Очистить
                clearWorld(); return;
            }
            // Кисть + / -
            float brushX = SW * 0.33f;
            float btnSize = ctrlH * 0.62f * 0.85f;
            float minusX = brushX + SW * 0.14f;
            float plusX  = minusX + btnSize + SW * 0.10f;
            if (x >= minusX && x <= minusX + btnSize) { if (brushRadius > 1) brushRadius--; return; }
            if (x >= plusX  && x <= plusX  + btnSize) { if (brushRadius < 8) brushRadius++; return; }

        } else if (y < row2Y) {
            // Ряд 1
            int idx = (int)(x / (SW / (float)ROW1.length));
            if (idx >= 0 && idx < ROW1.length) selectedTool = ROW1[idx];
        } else {
            // Ряд 2
            int idx = (int)(x / (SW / (float)ROW2.length));
            if (idx >= 0 && idx < ROW2.length) selectedTool = ROW2[idx];
        }
    }

    private void clearWorld() {
        world = new SandWorld(WORLD_W, WORLD_H);
        buildStartScene();
    }

    // ── Coords ───────────────────────────────────────────
    private int screenToWorldX(float sx) {
        return Math.max(0, Math.min(WORLD_W-1, (int)(sx / scaleX)));
    }
    private int screenToWorldY(float sy) {
        return Math.max(0, Math.min(WORLD_H-1, (int)(sy / scaleY)));
    }

    // ── Lifecycle ────────────────────────────────────────
    @Override public void surfaceCreated(SurfaceHolder h) {
        thread = new SimThread(); thread.start();
    }
    @Override public void surfaceDestroyed(SurfaceHolder h) {
        thread.running = false;
        try { thread.join(1000); } catch (InterruptedException ignored) {}
    }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
}
