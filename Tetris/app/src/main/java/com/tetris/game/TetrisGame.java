package com.tetris.game;

import java.util.Random;

public class TetrisGame {

    public static final int COLS = 10;
    public static final int ROWS = 20;

    // Все 7 тетромино
    private static final int[][][] SHAPES = {
        {{1,1,1,1}},                          // I
        {{1,1},{1,1}},                         // O
        {{0,1,0},{1,1,1}},                     // T
        {{1,0},{1,0},{1,1}},                   // L
        {{0,1},{0,1},{1,1}},                   // J
        {{0,1,1},{1,1,0}},                     // S
        {{1,1,0},{0,1,1}}                      // Z
    };

    // Цвета для каждой фигуры (ARGB)
    public static final int[] COLORS = {
        0xFF00F5FF,  // I — cyan
        0xFFFFE000,  // O — yellow
        0xFFBF00FF,  // T — purple
        0xFFFF8C00,  // L — orange
        0xFF0050FF,  // J — blue
        0xFF00E060,  // S — green
        0xFFFF1744,  // Z — red
    };

    public int[][] board = new int[ROWS][COLS]; // 0=empty, 1-7=color index
    public int[][] current;
    public int currentX, currentY, currentType;
    public int[][] next;
    public int nextType;
    public int score = 0;
    public int lines = 0;
    public int level = 1;
    public boolean gameOver = false;

    private Random rng = new Random();

    public TetrisGame() {
        nextType = rng.nextInt(7);
        next = SHAPES[nextType];
        spawn();
    }

    private void spawn() {
        currentType = nextType;
        current = copyShape(SHAPES[currentType]);
        currentX = COLS / 2 - current[0].length / 2;
        currentY = 0;
        nextType = rng.nextInt(7);
        next = SHAPES[nextType];
        if (!fits(current, currentX, currentY)) gameOver = true;
    }

    public boolean moveLeft()  { if (fits(current, currentX-1, currentY)) { currentX--; return true; } return false; }
    public boolean moveRight() { if (fits(current, currentX+1, currentY)) { currentX++; return true; } return false; }

    public boolean moveDown() {
        if (fits(current, currentX, currentY+1)) {
            currentY++;
            return true;
        }
        lock();
        return false;
    }

    public void hardDrop() {
        while (fits(current, currentX, currentY+1)) currentY++;
        score += 2;
        lock();
    }

    public void rotate() {
        int[][] rotated = rotateCW(current);
        // Wall kick
        int[] kicks = {0, -1, 1, -2, 2};
        for (int kick : kicks) {
            if (fits(rotated, currentX + kick, currentY)) {
                current = rotated;
                currentX += kick;
                return;
            }
        }
    }

    private void lock() {
        for (int r = 0; r < current.length; r++)
            for (int c = 0; c < current[r].length; c++)
                if (current[r][c] != 0)
                    board[currentY+r][currentX+c] = currentType + 1;
        int cleared = clearLines();
        lines += cleared;
        score += scoreForLines(cleared) * level;
        level = lines / 10 + 1;
        spawn();
    }

    private int clearLines() {
        int count = 0;
        for (int r = ROWS-1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) if (board[r][c] == 0) { full = false; break; }
            if (full) {
                for (int row = r; row > 0; row--) board[row] = board[row-1].clone();
                board[0] = new int[COLS];
                count++;
                r++;
            }
        }
        return count;
    }

    private int scoreForLines(int n) {
        switch (n) {
            case 1: return 100;
            case 2: return 300;
            case 3: return 500;
            case 4: return 800;
            default: return 0;
        }
    }

    public int ghostY() {
        int gy = currentY;
        while (fits(current, currentX, gy+1)) gy++;
        return gy;
    }

    private boolean fits(int[][] shape, int x, int y) {
        for (int r = 0; r < shape.length; r++)
            for (int c = 0; c < shape[r].length; c++)
                if (shape[r][c] != 0) {
                    int nx = x + c, ny = y + r;
                    if (nx < 0 || nx >= COLS || ny >= ROWS) return false;
                    if (ny >= 0 && board[ny][nx] != 0) return false;
                }
        return true;
    }

    private int[][] rotateCW(int[][] s) {
        int rows = s.length, cols = s[0].length;
        int[][] r = new int[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                r[j][rows-1-i] = s[i][j];
        return r;
    }

    private int[][] copyShape(int[][] s) {
        int[][] copy = new int[s.length][];
        for (int i = 0; i < s.length; i++) copy[i] = s[i].clone();
        return copy;
    }
}
