package com.tetris;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.view.*;
import java.util.Random;

public class TetrisUltra extends Activity {

    private V gameView;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        gameView = new V(this);
        setContentView(gameView);
    }

    @Override protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pauseGame();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.resumeGame();
    }

    static class V extends SurfaceView implements SurfaceHolder.Callback {
        static final int W = 10, H = 20;
        static final short[] SH = {0x00F0, 0x0660, 0x04E0, 0x08C4, 0x04C8, 0x06C0, 0x0C60};
        static final int[] COL = {0xFF00F5FF, 0xFFFFE000, 0xFFBF00FF, 0xFFFF8C00, 0xFF0050FF, 0xFF00E060, 0xFFFF1744};

        byte[] brd = new byte[W * H];
        short cur, nxt;
        int cx, cy, ct, nt;
        int sc, ln, lv = 1, cmb;
        boolean over;
        int lc;

        int st; // 0 = Start, 1 = Playing, 2 = Pause, 3 = GameOver
        int best;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF r = new RectF();
        float cs, bx, by, px;
        int cw, ch;
        LinearGradient bg;

        float startX, startY, lastX, lastY;
        float accumDx, accumDy;
        long touchTime;

        float[] ppx = new float[50], ppy = new float[50];
        float[] pvx = new float[50], pvy = new float[50], plf = new float[50];
        int[] pcl = new int[50];
        int pc;

        AudioTrack trRot, trDrp, trLn, trGo;

        Thread gt;
        volatile boolean run;
        long ld, di = 800;

        Random rng = new Random();

        V(Context c) {
            super(c);
            getHolder().addCallback(this);
            setFocusable(true);
            best = c.getSharedPreferences("t", 0).getInt("b", 0);
            initAudio();
            initGame();
        }

        void initAudio() {
            try {
                AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                AudioFormat af = new AudioFormat.Builder()
                    .setSampleRate(22050)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();

                short[] bRot = tone(880, 50, 0.4f);
                short[] bDrp = sweep(300, 100, 100, 0.5f);
                short[] bLn  = sweep(400, 900, 200, 0.5f);
                short[] bGo  = sweep(600, 100, 600, 0.5f);

                trRot = buildTrack(aa, af, bRot);
                trDrp = buildTrack(aa, af, bDrp);
                trLn  = buildTrack(aa, af, bLn);
                trGo  = buildTrack(aa, af, bGo);
            } catch (Exception ignored) {}
        }

        AudioTrack buildTrack(AudioAttributes aa, AudioFormat af, short[] b) {
            try {
                AudioTrack t = new AudioTrack.Builder()
                    .setAudioAttributes(aa).setAudioFormat(af)
                    .setBufferSizeInBytes(b.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build();
                t.write(b, 0, b.length);
                return t;
            } catch (Exception e) { return null; }
        }

        short[] tone(float f, int ms, float v) {
            int n = 22050 * ms / 1000;
            short[] b = new short[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / 22050;
                b[i] = (short) (Math.sin(2 * Math.PI * f * t) * 32000 * v * (1f - (float) i / n));
            }
            return b;
        }

        short[] sweep(float f1, float f2, int ms, float v) {
            int n = 22050 * ms / 1000;
            short[] b = new short[n];
            double ph = 0;
            for (int i = 0; i < n; i++) {
                float t = (float) i / n;
                ph += 2 * Math.PI * (f1 + (f2 - f1) * t) / 22050;
                b[i] = (short) (Math.sin(ph) * 32000 * v * (1f - t));
            }
            return b;
        }

        void snd(AudioTrack t) {
            if (t == null) return;
            try {
                t.stop();
                t.reloadStaticData();
                t.play();
            } catch (Exception ignored) {}
        }

        void initGame() {
            brd = new byte[W * H];
            nt = rng.nextInt(7);
            nxt = SH[nt];
            spawn();
            sc = ln = cmb = 0;
            lv = 1;
            over = false;
            di = 800;
            ld = System.currentTimeMillis();
        }

        void pauseGame() {
            if (st == 1) st = 2;
        }

        void resumeGame() {
            ld = System.currentTimeMillis();
        }

        void spawn() {
            ct = nt;
            cur = SH[ct];
            cx = 3; cy = 0;
            nt = rng.nextInt(7);
            nxt = SH[nt];
            if (!fits(cur, cx, cy)) over = true;
        }

        static boolean bit(short s, int r, int c) {
            return ((s >> (15 - (r * 4 + c))) & 1) == 1;
        }

        boolean fits(short s, int x, int y) {
            for (int rr = 0; rr < 4; rr++)
                for (int cc = 0; cc < 4; cc++)
                    if (bit(s, rr, cc)) {
                        int nx = x + cc, ny = y + rr;
                        if (nx < 0 || nx >= W || ny >= H) return false;
                        if (ny >= 0 && brd[ny * W + nx] != 0) return false;
                    }
            return true;
        }

        short rot(short s) {
            short rs = 0;
            for (int rr = 0; rr < 4; rr++)
                for (int cc = 0; cc < 4; cc++)
                    if (bit(s, rr, cc))
                        rs |= (1 << (15 - (cc * 4 + (3 - rr))));
            return rs;
        }

        boolean mL() { if (fits(cur, cx - 1, cy)) { cx--; return true; } return false; }
        boolean mR() { if (fits(cur, cx + 1, cy)) { cx++; return true; } return false; }
        boolean mD() { if (fits(cur, cx, cy + 1)) { cy++; return true; } lock(); return false; }

        void hDrop() {
            int d = 0;
            while (fits(cur, cx, cy + 1)) { cy++; d++; }
            sc += d * 2;
            lock();
        }

        boolean doRot() {
            short rt = rot(cur);
            int[] k = {0, -1, 1, -2, 2};
            for (int kk : k)
                if (fits(rt, cx + kk, cy)) { cur = rt; cx += kk; return true; }
            return false;
        }

        void lock() {
            for (int rr = 0; rr < 4; rr++)
                for (int cc = 0; cc < 4; cc++)
                    if (bit(cur, rr, cc)) {
                        int ny = cy + rr, nx = cx + cc;
                        if (ny >= 0 && ny < H && nx >= 0 && nx < W)
                            brd[ny * W + nx] = (byte) (ct + 1);
                    }
            lc = clr();
            if (lc > 0) { cmb++; ln += lc; sc += scr(lc) * lv + cmb * 50; }
            else cmb = 0;
            lv = ln / 10 + 1;
            spawn();
        }

        int clr() {
            int cnt = 0;
            for (int rr = H - 1; rr >= 0; rr--) {
                boolean full = true;
                for (int cc = 0; cc < W; cc++) if (brd[rr * W + cc] == 0) { full = false; break; }
                if (full) {
                    System.arraycopy(brd, 0, brd, W, rr * W);
                    for (int cc = 0; cc < W; cc++) brd[cc] = 0;
                    cnt++; rr++;
                }
            }
            return cnt;
        }

        int scr(int n) {
            return n == 1 ? 100 : n == 2 ? 300 : n == 3 ? 500 : n == 4 ? 800 : 0;
        }

        int gY() {
            int gy = cy;
            while (fits(cur, cx, gy + 1)) gy++;
            return gy;
        }

        void spawnP() {
            pc = 0;
            for (int i = 0; i < 40 && pc < 50; i++) {
                ppx[pc] = bx + rng.nextFloat() * cs * W;
                ppy[pc] = by + cs * (H / 2);
                pvx[pc] = (rng.nextFloat() - 0.5f) * 700;
                pvy[pc] = -rng.nextFloat() * 500;
                pcl[pc] = COL[rng.nextInt(7)];
                plf[pc] = 1f;
                pc++;
            }
        }

        void updP(float dt) {
            int al = 0;
            for (int i = 0; i < pc; i++) {
                ppx[i] += pvx[i] * dt;
                ppy[i] += pvy[i] * dt;
                pvy[i] += 1000 * dt;
                plf[i] -= dt * 1.5f;
                if (plf[i] > 0) {
                    ppx[al] = ppx[i]; ppy[al] = ppy[i];
                    pvx[al] = pvx[i]; pvy[al] = pvy[i];
                    pcl[al] = pcl[i]; plf[al] = plf[i];
                    al++;
                }
            }
            pc = al;
        }

        @Override public void surfaceCreated(SurfaceHolder h) {
            run = true;
            ld = System.currentTimeMillis();
            gt = new Thread(new Runnable() {
                @Override public void run() {
                    while (run) {
                        if (st == 1 && !over) {
                            long now = System.currentTimeMillis();
                            if (now - ld > di) {
                                if (!mD()) snd(trDrp);
                                di = Math.max(80, 800 - (lv - 1) * 70);
                                if (lc > 0) { snd(trLn); spawnP(); lc = 0; }
                                ld = now;
                            }
                        } else if (st == 1 && over) {
                            st = 3;
                            snd(trGo);
                            if (sc > best) {
                                best = sc;
                                getContext().getSharedPreferences("t", 0).edit().putInt("b", best).apply();
                            }
                        }
                        updP(0.016f);
                        draw();
                        try { Thread.sleep(16); } catch (Exception ignored) {}
                    }
                }
            });
            gt.start();
        }

        @Override public void surfaceDestroyed(SurfaceHolder h) {
            run = false;
            try { gt.join(); } catch (Exception ignored) {}
            releaseAudio();
        }

        void releaseAudio() {
            releaseTrack(trRot);
            releaseTrack(trDrp);
            releaseTrack(trLn);
            releaseTrack(trGo);
        }

        void releaseTrack(AudioTrack t) {
            if (t != null) {
                try { t.stop(); t.release(); } catch (Exception ignored) {}
            }
        }

        @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) { cw = 0; ch = 0; }

        void draw() {
            SurfaceHolder h = getHolder();
            if (!h.getSurface().isValid()) return;
            Canvas c = h.lockCanvas();
            if (c == null) return;
            try {
                int ww = c.getWidth(), hh = c.getHeight();
                if (cw != ww || ch != hh) {
                    cw = ww; ch = hh;
                    cs = Math.min((hh * 0.92f) / H, (ww * 0.92f) / 16f);
                    float totalW = cs * 16f;
                    bx = (ww - totalW) / 2f + cs * 0.2f;
                    by = (hh - cs * H) / 2f;
                    px = bx + cs * 10.4f;
                    bg = new LinearGradient(0, 0, 0, hh, 0xFF0D0D1A, 0xFF1A1A2E, Shader.TileMode.CLAMP);
                }

                p.setShader(bg);
                c.drawRect(0, 0, ww, hh, p);
                p.setShader(null);

                if (st == 0) drawStart(c, ww, hh);
                else {
                    drawGame(c);
                    if (st == 2) drawPause(c, ww, hh);
                    else if (st == 3) drawOver(c, ww, hh);
                }
                drawPart(c);
            } finally { h.unlockCanvasAndPost(c); }
        }

        void drawStart(Canvas c, int w, int h) {
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(cs * 2);
            p.setColor(0xFF00F5FF);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("TETRIS", w / 2f, h / 2f - cs * 2, p);
            p.setTextSize(cs * 0.7f);
            p.setColor(0xFFFFFFFF);
            c.drawText("Best: " + best, w / 2f, h / 2f, p);
            p.setTextSize(cs * 0.5f);
            p.setColor(0xFFAAAAAA);
            if (System.currentTimeMillis() % 1000 < 700) c.drawText("Tap to start!", w / 2f, h / 2f + cs * 2, p);
        }

        void drawGame(Canvas c) {
            p.setColor(0xFF111120);
            p.setStyle(Paint.Style.FILL);
            r.set(bx - 2, by - 2, bx + cs * W + 2, by + cs * H + 2);
            c.drawRoundRect(r, 8, 8, p);

            p.setColor(0x18FFFFFF);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(0.5f);
            for (int rr = 0; rr <= H; rr++) c.drawLine(bx, by + rr * cs, bx + cs * W, by + rr * cs, p);
            for (int cc = 0; cc <= W; cc++) c.drawLine(bx + cc * cs, by, bx + cc * cs, by + cs * H, p);
            p.setStyle(Paint.Style.FILL);

            for (int rr = 0; rr < H; rr++)
                for (int cc = 0; cc < W; cc++) {
                    byte v = brd[rr * W + cc];
                    if (v != 0) drawCell(c, cc, rr, COL[v - 1]);
                }

            int gy = gY();
            if (gy != cy) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f);
                p.setColor(COL[ct]);
                p.setAlpha(60);
                for (int rr = 0; rr < 4; rr++)
                    for (int cc = 0; cc < 4; cc++)
                        if (bit(cur, rr, cc)) {
                            float x = bx + (cx + cc) * cs, y = by + (gy + rr) * cs;
                            float pd = cs * 0.08f;
                            r.set(x + pd, y + pd, x + cs - pd, y + cs - pd);
                            c.drawRoundRect(r, 4, 4, p);
                        }
                p.setAlpha(255);
                p.setStyle(Paint.Style.FILL);
            }

            for (int rr = 0; rr < 4; rr++)
                for (int cc = 0; cc < 4; cc++)
                    if (bit(cur, rr, cc)) drawCell(c, cx + cc, cy + rr, COL[ct]);

            float pw = cs * 5.2f;
            p.setColor(0xDD151525);
            r.set(px, by, px + pw, by + cs * 12.2f);
            c.drawRoundRect(r, 12, 12, p);

            float cx2 = px + pw / 2, y = by + cs * 0.8f;
            drawLbl(c, "NEXT", cx2, y);
            drawPrev(c, nxt, nt, cx2, y + cs * 0.4f);

            y += cs * 3.4f;
            drawSt(c, "SCORE", "" + sc, cx2, y);
            drawSt(c, "LINES", "" + ln, cx2, y + cs * 1.5f);
            drawSt(c, "LEVEL", "" + lv, cx2, y + cs * 3.0f);
            drawSt(c, "BEST", "" + best, cx2, y + cs * 4.5f);

            // Pause Button
            float pbY = by + cs * 10.3f;
            p.setColor(st == 2 ? 0xFF2A2A50 : 0xFF1F1F3D);
            r.set(px + cs * 0.4f, pbY, px + pw - cs * 0.4f, pbY + cs * 1.4f);
            c.drawRoundRect(r, 6, 6, p);
            p.setColor(0xFF00F5FF);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(cs * 0.45f);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(st == 2 ? "RESUME" : "PAUSE", cx2, pbY + cs * 0.88f, p);
        }

        void drawCell(Canvas c, int col, int row, int clr) {
            float x = bx + col * cs, y = by + row * cs, pd = cs * 0.05f;
            r.set(x + pd, y + pd, x + cs - pd, y + cs - pd);
            p.setColor(clr);
            c.drawRoundRect(r, 4, 4, p);
            p.setColor(0x55FFFFFF);
            r.set(x + pd, y + pd, x + cs - pd, y + pd + cs * 0.22f);
            c.drawRoundRect(r, 4, 2, p);
        }

        void drawLbl(Canvas c, String t, float cx2, float y) {
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(cs * 0.5f);
            p.setColor(0xFF888888);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(t, cx2, y, p);
        }

        void drawPrev(Canvas c, short sh, int tp, float cx2, float cy) {
            int clr = COL[tp];
            float ps = cs * 0.55f;
            int minC = 4, maxC = 0, minR = 4, maxR = 0;
            for (int rr = 0; rr < 4; rr++)
                for (int cc = 0; cc < 4; cc++)
                    if (bit(sh, rr, cc)) {
                        minR = Math.min(minR, rr);
                        maxR = Math.max(maxR, rr);
                        minC = Math.min(minC, cc);
                        maxC = Math.max(maxC, cc);
                    }
            int sw = maxC - minC + 1;
            float ox = cx2 - (sw * ps) / 2f, oy = cy + cs * 0.2f;
            for (int rr = minR; rr <= maxR; rr++)
                for (int cc = minC; cc <= maxC; cc++)
                    if (bit(sh, rr, cc)) {
                        float x = ox + (cc - minC) * ps, y = oy + (rr - minR) * ps, pd = ps * 0.06f;
                        r.set(x + pd, y + pd, x + ps - pd, y + ps - pd);
                        p.setColor(clr);
                        c.drawRoundRect(r, 3, 3, p);
                    }
        }

        void drawSt(Canvas c, String lb, String vl, float cx2, float y) {
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(cs * 0.35f);
            p.setColor(0xFF666666);
            p.setTypeface(Typeface.DEFAULT);
            c.drawText(lb, cx2, y, p);
            p.setTextSize(cs * 0.55f);
            p.setColor(0xFFFFFFFF);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(vl, cx2, y + cs * 0.55f, p);
        }

        void drawPart(Canvas c) {
            for (int i = 0; i < pc; i++) {
                p.setColor(pcl[i]);
                p.setAlpha((int) (plf[i] * 255));
                c.drawCircle(ppx[i], ppy[i], cs * 0.15f * plf[i], p);
            }
            p.setAlpha(255);
        }

        void drawPause(Canvas c, int w, int h) {
            p.setColor(0xBB000000);
            c.drawRect(0, 0, w, h, p);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(cs * 1.3f);
            p.setColor(0xFF00F5FF);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("PAUSE", w / 2f, h / 2f, p);
            p.setTextSize(cs * 0.5f);
            p.setColor(0xFFAAAAAA);
            c.drawText("Tap to continue", w / 2f, h / 2f + cs * 1.2f, p);
        }

        void drawOver(Canvas c, int w, int h) {
            p.setColor(0xCC000000);
            c.drawRect(0, 0, w, h, p);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(cs * 1.3f);
            p.setColor(0xFFFF1744);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("GAME OVER", w / 2f, h / 2f - cs * 1.5f, p);
            p.setTextSize(cs * 0.7f);
            p.setColor(0xFFFFFFFF);
            c.drawText("Score: " + sc, w / 2f, h / 2f, p);
            if (sc >= best) {
                p.setTextSize(cs * 0.55f);
                p.setColor(0xFFFFE000);
                c.drawText("\u2605 NEW BEST! \u2605", w / 2f, h / 2f + cs, p);
            }
            p.setTextSize(cs * 0.5f);
            p.setColor(0xFFAAAAAA);
            if (System.currentTimeMillis() % 1000 < 700) c.drawText("Tap to restart", w / 2f, h / 2f + cs * 2.2f, p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            long now = System.currentTimeMillis();

            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = lastX = x;
                    startY = lastY = y;
                    accumDx = 0; accumDy = 0;
                    touchTime = now;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastX;
                    float dy = y - lastY;
                    lastX = x; lastY = y;

                    if (st == 1 && !over) {
                        accumDx += dx;
                        float stepW = cs * 0.9f;
                        while (accumDx >= stepW) {
                            mR();
                            accumDx -= stepW;
                        }
                        while (accumDx <= -stepW) {
                            mL();
                            accumDx += stepW;
                        }

                        accumDy += dy;
                        float stepH = cs * 1.2f;
                        while (accumDy >= stepH) {
                            mD();
                            accumDy -= stepH;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    float totalDx = x - startX;
                    float totalDy = y - startY;
                    float totalDist = (float) Math.hypot(totalDx, totalDy);
                    long dt = now - touchTime;

                    float pw = cs * 5.2f;
                    float pbY = by + cs * 10.3f;
                    boolean clickedPause = (st == 1 || st == 2) && x >= px && x <= px + pw && y >= pbY - cs*0.5f && y <= pbY + cs*2.0f;

                    if (clickedPause) {
                        st = (st == 1) ? 2 : 1;
                        ld = now;
                        return true;
                    }

                    if (st == 0) {
                        if (totalDist < cs * 1.5f) {
                            st = 1;
                            ld = now;
                        }
                    } else if (st == 1) {
                        if (totalDist < cs * 0.5f && dt < 300) {
                            doRot();
                            snd(trRot);
                        } else if (totalDy > cs * 2.5f && dt < 300 && totalDy / dt > 0.4f) {
                            hDrop();
                            snd(trDrp);
                        }
                    } else if (st == 2) {
                        st = 1;
                        ld = now;
                    } else if (st == 3) {
                        if (totalDist < cs * 1.5f) {
                            initGame();
                            st = 1;
                            ld = now;
                        }
                    }
                    break;
            }
            return true;
        }
    }
}
