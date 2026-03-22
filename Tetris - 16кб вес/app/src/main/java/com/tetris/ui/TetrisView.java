package com.tetris.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.tetris.game.TetrisGame;

public class TetrisView extends SurfaceView implements SurfaceHolder.Callback {

    private TetrisGame game;
    private GameThread thread;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Handler handler = new Handler(Looper.getMainLooper());

    // Размеры
    private float cellSize;
    private float boardLeft, boardTop;
    private float panelX;

    // Свайп
    private float touchStartX, touchStartY;
    private long touchStartTime;
    private static final float SWIPE_THRESHOLD = 60f;
    private static final long TAP_MAX_MS = 200;

    // Скорость падения (мс)
    private long dropInterval = 800;

    public interface OnScoreListener { void onScore(int score, int lines, int level); }
    public interface OnGameOver { void onGameOver(int score); }
    private OnScoreListener scoreListener;
    private OnGameOver gameOverListener;

    public TetrisView(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);
        game = new TetrisGame();
    }

    public void setScoreListener(OnScoreListener l) { scoreListener = l; }
    public void setGameOverListener(OnGameOver l) { gameOverListener = l; }

    // ── Game Loop ──────────────────────────────────────────
    private class GameThread extends Thread {
        private volatile boolean running = true;
        private long lastDrop = System.currentTimeMillis();

        @Override public void run() {
            while (running) {
                long now = System.currentTimeMillis();
                if (!game.gameOver && now - lastDrop > dropInterval) {
                    game.moveDown();
                    dropInterval = Math.max(100, 800 - (game.level - 1) * 70L);
                    lastDrop = now;
                    notifyScore();
                }
                draw();
                try { Thread.sleep(16); } catch (InterruptedException ignored) {}
            }
        }

        void stopLoop() { running = false; }
    }

    private void notifyScore() {
        if (scoreListener != null)
            handler.post(new Runnable() {
                @Override public void run() {
                    scoreListener.onScore(game.score, game.lines, game.level);
                    if (game.gameOver && gameOverListener != null)
                        gameOverListener.onGameOver(game.score);
                }
            });
    }

    // ── Drawing ────────────────────────────────────────────
    private void draw() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            drawFrame(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawFrame(Canvas canvas) {
        int w = canvas.getWidth(), h = canvas.getHeight();

        // Фон — тёмный градиент
        paint.setShader(new LinearGradient(0, 0, 0, h,
                0xFF0D0D1A, 0xFF1A1A2E, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Размер клетки
        cellSize = Math.min((h * 0.95f) / TetrisGame.ROWS, (w * 0.6f) / TetrisGame.COLS);
        float boardW = cellSize * TetrisGame.COLS;
        float boardH = cellSize * TetrisGame.ROWS;
        boardLeft = (w * 0.05f);
        boardTop  = (h - boardH) / 2f;
        panelX = boardLeft + boardW + cellSize * 0.8f;

        drawBoard(canvas);
        drawGhost(canvas);
        drawCurrentPiece(canvas);
        drawPanel(canvas);

        if (game.gameOver) drawGameOver(canvas, w, h);
    }

    private void drawBoard(Canvas canvas) {
        // Фон поля
        paint.setColor(0xFF111120);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(boardLeft-2, boardTop-2,
                boardLeft + cellSize*TetrisGame.COLS+2,
                boardTop  + cellSize*TetrisGame.ROWS+2), 8, 8, paint);

        // Сетка
        paint.setColor(0x22FFFFFF);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f);
        for (int r = 0; r <= TetrisGame.ROWS; r++)
            canvas.drawLine(boardLeft, boardTop + r*cellSize,
                    boardLeft + cellSize*TetrisGame.COLS, boardTop + r*cellSize, paint);
        for (int c = 0; c <= TetrisGame.COLS; c++)
            canvas.drawLine(boardLeft + c*cellSize, boardTop,
                    boardLeft + c*cellSize, boardTop + cellSize*TetrisGame.ROWS, paint);

        // Блоки
        for (int r = 0; r < TetrisGame.ROWS; r++)
            for (int c = 0; c < TetrisGame.COLS; c++)
                if (game.board[r][c] != 0)
                    drawCell(canvas, c, r, TetrisGame.COLORS[game.board[r][c]-1], 1f);
    }

    private void drawGhost(Canvas canvas) {
        int gy = game.ghostY();
        if (gy == game.currentY) return;
        int color = TetrisGame.COLORS[game.currentType];
        for (int r = 0; r < game.current.length; r++)
            for (int c = 0; c < game.current[r].length; c++)
                if (game.current[r][c] != 0)
                    drawCellGhost(canvas, game.currentX+c, gy+r, color);
    }

    private void drawCurrentPiece(Canvas canvas) {
        int color = TetrisGame.COLORS[game.currentType];
        for (int r = 0; r < game.current.length; r++)
            for (int c = 0; c < game.current[r].length; c++)
                if (game.current[r][c] != 0)
                    drawCell(canvas, game.currentX+c, game.currentY+r, color, 1f);
    }

    private void drawCell(Canvas canvas, int col, int row, int color, float alpha) {
        float x = boardLeft + col * cellSize;
        float y = boardTop  + row * cellSize;
        float pad = cellSize * 0.05f;
        RectF rect = new RectF(x+pad, y+pad, x+cellSize-pad, y+cellSize-pad);

        // Fill
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha((int)(255*alpha));
        paint.setColor(color);
        canvas.drawRoundRect(rect, 4, 4, paint);

        // Highlight top-left
        paint.setColor(0x66FFFFFF);
        canvas.drawRoundRect(new RectF(x+pad, y+pad, x+cellSize-pad, y+pad+cellSize*0.25f), 4, 2, paint);

        // Border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(darken(color));
        paint.setAlpha((int)(255*alpha));
        canvas.drawRoundRect(rect, 4, 4, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCellGhost(Canvas canvas, int col, int row, int color) {
        float x = boardLeft + col * cellSize;
        float y = boardTop  + row * cellSize;
        float pad = cellSize * 0.05f;
        RectF rect = new RectF(x+pad, y+pad, x+cellSize-pad, y+cellSize-pad);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(color);
        paint.setAlpha(80);
        canvas.drawRoundRect(rect, 4, 4, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPanel(Canvas canvas) {
        float pw = cellSize * 4.5f;
        paint.setColor(0xFF1A1A2E);
        canvas.drawRoundRect(new RectF(panelX, boardTop, panelX+pw, boardTop+cellSize*8), 12, 12, paint);

        // NEXT label
        paint.setColor(0xFFAAAAAA);
        paint.setTextSize(cellSize * 0.55f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("NEXT", panelX + pw/2, boardTop + cellSize*0.9f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("NEXT", panelX + pw/2, boardTop + cellSize*0.9f, paint);

        // Next piece preview
        drawNextPreview(canvas, panelX + pw/2, boardTop + cellSize*1.2f);

        // Score / Lines / Level
        float sy = boardTop + cellSize * 4.5f;
        float spacing = cellSize * 1.1f;
        drawStat(canvas, "SCORE", String.valueOf(game.score), panelX + pw/2, sy);
        drawStat(canvas, "LINES", String.valueOf(game.lines), panelX + pw/2, sy + spacing);
        drawStat(canvas, "LEVEL", String.valueOf(game.level), panelX + pw/2, sy + spacing*2);
    }

    private void drawNextPreview(Canvas canvas, float cx, float cy) {
        int[][] shape = game.next;
        int color = TetrisGame.COLORS[game.nextType];
        float previewCell = cellSize * 0.75f;
        float ox = cx - (shape[0].length * previewCell) / 2f;
        float oy = cy;
        for (int r = 0; r < shape.length; r++)
            for (int c = 0; c < shape[r].length; c++)
                if (shape[r][c] != 0) {
                    float x = ox + c * previewCell;
                    float y = oy + r * previewCell;
                    float pad = previewCell * 0.05f;
                    RectF rect = new RectF(x+pad, y+pad, x+previewCell-pad, y+previewCell-pad);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(color);
                    canvas.drawRoundRect(rect, 3, 3, paint);
                    paint.setColor(0x55FFFFFF);
                    canvas.drawRoundRect(new RectF(x+pad, y+pad, x+previewCell-pad, y+pad+previewCell*0.25f), 3, 1, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1f);
                    paint.setColor(darken(color));
                    canvas.drawRoundRect(rect, 3, 3, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
    }

    private void drawStat(Canvas canvas, String label, String value, float cx, float y) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize * 0.38f);
        paint.setColor(0xFF888888);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(label, cx, y, paint);
        paint.setTextSize(cellSize * 0.65f);
        paint.setColor(0xFFFFFFFF);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(value, cx, y + cellSize * 0.72f, paint);
    }

    private void drawGameOver(Canvas canvas, int w, int h) {
        paint.setColor(0xCC000000);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize * 1.2f);
        paint.setColor(0xFFFF1744);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("GAME OVER", w/2f, h/2f - cellSize, paint);
        paint.setTextSize(cellSize * 0.65f);
        paint.setColor(0xFFFFFFFF);
        canvas.drawText("Score: " + game.score, w/2f, h/2f + cellSize*0.3f, paint);
        paint.setTextSize(cellSize * 0.5f);
        paint.setColor(0xFFAAAAAA);
        canvas.drawText("Tap to restart", w/2f, h/2f + cellSize*1.3f, paint);
    }

    private int darken(int color) {
        int r = (int)(((color>>16)&0xFF)*0.6f);
        int g = (int)(((color>>8)&0xFF)*0.6f);
        int b = (int)((color&0xFF)*0.6f);
        return 0xFF000000 | (r<<16) | (g<<8) | b;
    }

    // ── Touch / Swipe ──────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = e.getX();
                touchStartY = e.getY();
                touchStartTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                if (game.gameOver) { restart(); return true; }
                float dx = e.getX() - touchStartX;
                float dy = e.getY() - touchStartY;
                long dt = System.currentTimeMillis() - touchStartTime;
                if (Math.abs(dx) < 20 && Math.abs(dy) < 20 && dt < TAP_MAX_MS) {
                    game.rotate();
                } else if (Math.abs(dx) > Math.abs(dy)) {
                    if (Math.abs(dx) > SWIPE_THRESHOLD)
                        if (dx > 0) game.moveRight(); else game.moveLeft();
                } else {
                    if (dy > SWIPE_THRESHOLD) game.hardDrop();
                    else if (dy < -SWIPE_THRESHOLD) game.rotate();
                }
                break;
        }
        return true;
    }

    private void restart() {
        game = new TetrisGame();
        dropInterval = 800;
    }

    // ── Lifecycle ──────────────────────────────────────────
    @Override public void surfaceCreated(SurfaceHolder h) {
        thread = new GameThread();
        thread.start();
    }

    @Override public void surfaceDestroyed(SurfaceHolder h) {
        thread.stopLoop();
        try { thread.join(); } catch (InterruptedException ignored) {}
    }

    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
}
