package com.sandbox.game;

import java.util.Random;

public class SandWorld {

    public final int W, H;
    public final int[] grid;      // тип частицы
    public final int[] age;       // возраст (для роста растений, горения)
    public final boolean[] dirty; // обновлялась ли клетка в этот тик
    private boolean[] updated;    // чтобы не обновлять дважды за тик

    private Random rng = new Random();
    private int tick = 0;

    public SandWorld(int w, int h) {
        W = w; H = h;
        grid    = new int[W * H];
        age     = new int[W * H];
        dirty   = new boolean[W * H];
        updated = new boolean[W * H];
    }

    public int idx(int x, int y) { return y * W + x; }
    public boolean inBounds(int x, int y) { return x>=0&&x<W&&y>=0&&y<H; }

    public int get(int x, int y) {
        if (!inBounds(x,y)) return Particle.WALL;
        return grid[idx(x,y)];
    }

    public void set(int x, int y, int type) {
        if (!inBounds(x,y)) return;
        int i = idx(x,y);
        grid[i] = type;
        age[i] = 0;
        dirty[i] = true;
    }

    private void swap(int x1,int y1,int x2,int y2) {
        int i1=idx(x1,y1), i2=idx(x2,y2);
        int t=grid[i1]; grid[i1]=grid[i2]; grid[i2]=t;
        t=age[i1]; age[i1]=age[i2]; age[i2]=t;
        dirty[i1]=true; dirty[i2]=true;
    }

    private void replace(int x,int y,int type) {
        int i=idx(x,y);
        grid[i]=type; age[i]=0; dirty[i]=true;
    }

    public void step() {
        tick++;
        java.util.Arrays.fill(updated, false);

        // Обходим снизу вверх, чередуя направление по x
        boolean leftFirst = (tick % 2 == 0);
        for (int y = H-1; y >= 0; y--) {
            if (leftFirst) {
                for (int x = 0; x < W; x++) updateCell(x, y);
            } else {
                for (int x = W-1; x >= 0; x--) updateCell(x, y);
            }
        }
    }

    private void updateCell(int x, int y) {
        int i = idx(x,y);
        if (updated[i]) return;
        int type = grid[i];
        if (type == Particle.EMPTY) return;

        updated[i] = true;

        switch (type) {
            case Particle.SAND:
            case Particle.FLOUR:
            case Particle.SALT:
            case Particle.YEAST:
            case Particle.ASH:
                updatePowder(x, y); break;
            case Particle.WHEAT_SEED:
            case Particle.POTATO_SEED:
                updateSeed(x, y); break;
            case Particle.WATER:
                updateWater(x, y); break;
            case Particle.FIRE:
                updateFire(x, y); break;
            case Particle.STEAM:
                updateSteam(x, y); break;
            case Particle.HEAT:
                updateHeat(x, y); break;
            case Particle.STOVE:
                updateStove(x, y); break;
            case Particle.OVEN:
                updateOven(x, y); break;
            case Particle.WHEAT:
                updateWheat(x, y); break;
            default:
                checkReactions(x, y); break;
        }
    }

    // ── Порошки (песок, мука, соль, дрожжи, пепел) ──────
    private void updatePowder(int x, int y) {
        // Вниз
        if (canDisplace(x,y,x,y+1)) { swap(x,y,x,y+1); return; }
        // Вниз-влево или вниз-вправо (случайно)
        int d = rng.nextBoolean() ? 1 : -1;
        if (canDisplace(x,y,x+d,y+1)) { swap(x,y,x+d,y+1); return; }
        if (canDisplace(x,y,x-d,y+1)) { swap(x,y,x-d,y+1); }
    }

    // ── Вода ─────────────────────────────────────────────
    private void updateWater(int x, int y) {
        if (canDisplace(x,y,x,y+1)) { swap(x,y,x,y+1); return; }
        int d = rng.nextBoolean() ? 1 : -1;
        if (canDisplace(x,y,x+d,y+1)) { swap(x,y,x+d,y+1); return; }
        // Течёт по горизонтали
        if (canDisplace(x,y,x+d,y)) { swap(x,y,x+d,y); return; }
        if (canDisplace(x,y,x-d,y)) { swap(x,y,x-d,y); }
    }

    // ── Огонь ─────────────────────────────────────────────
    private void updateFire(int x, int y) {
        age[idx(x,y)]++;
        if (age[idx(x,y)] > 40 + rng.nextInt(30)) {
            replace(x, y, rng.nextInt(3)==0 ? Particle.ASH : Particle.EMPTY);
            return;
        }
        // Поднимается вверх
        if (get(x,y-1)==Particle.EMPTY && rng.nextInt(3)==0) { swap(x,y,x,y-1); return; }
        // Распространяет жар на соседей
        spreadHeat(x,y);
    }

    private void spreadHeat(int x, int y) {
        int[][] dirs = {{0,-1},{0,1},{-1,0},{1,0}};
        for (int[] d : dirs) {
            int nx=x+d[0], ny=y+d[1];
            if (!inBounds(nx,ny)) continue;
            int n = get(nx,ny);
            // Поджигаем деревянное (тесто может гореть)
            if (n==Particle.DOUGH || n==Particle.DOUGH_Y) {
                if (rng.nextInt(20)==0) replace(nx,ny,Particle.FIRE);
            }
            // Нагрев — выпекаем тесто
            if ((n==Particle.DOUGH) && rng.nextInt(30)==0) replace(nx,ny,Particle.BREAD);
            if ((n==Particle.DOUGH_Y) && rng.nextInt(30)==0) replace(nx,ny,Particle.BREAD_PUFF);
            // Нагрев картошки
            if (n==Particle.POTATO && rng.nextInt(25)==0) replace(nx,ny,Particle.FRIED_POTATO);
            // Вода → пар
            if (n==Particle.WATER && rng.nextInt(10)==0) replace(nx,ny,Particle.STEAM);
        }
    }

    // ── Пар ───────────────────────────────────────────────
    private void updateSteam(int x, int y) {
        age[idx(x,y)]++;
        if (age[idx(x,y)] > 60 + rng.nextInt(40)) {
            replace(x, y, Particle.EMPTY); return;
        }
        // Поднимается
        if (get(x,y-1)==Particle.EMPTY && rng.nextInt(2)==0) { swap(x,y,x,y-1); return; }
        int d = rng.nextBoolean() ? 1 : -1;
        if (get(x+d,y)==Particle.EMPTY) { swap(x,y,x+d,y); }
    }

    // ── Жар (от плиты/духовки) ────────────────────────────
    private void updateHeat(int x, int y) {
        age[idx(x,y)]++;
        if (age[idx(x,y)] > 5) { replace(x,y,Particle.EMPTY); return; }
        spreadHeat(x,y);
        // Поднимается
        if (get(x,y-1)==Particle.EMPTY) { swap(x,y,x,y-1); }
    }

    // ── Плита (источник огня/жара) ────────────────────────
    private void updateStove(int x, int y) {
        // Каждый тик генерирует огонь/жар сверху
        if (tick % 3 == 0 && get(x,y-1)==Particle.EMPTY) {
            int i = idx(x,y-1);
            grid[i] = rng.nextInt(3)==0 ? Particle.FIRE : Particle.HEAT;
            age[i] = 0;
            dirty[i] = true;
        }
        spreadHeat(x,y);
    }

    // ── Духовка (нагревает внутреннее пространство) ───────
    private void updateOven(int x, int y) {
        // Создаёт жар внутри (над собой)
        if (tick % 4 == 0) {
            for (int dy = 1; dy <= 3; dy++) {
                if (!inBounds(x, y-dy)) break;
                int n = get(x, y-dy);
                if (n==Particle.EMPTY) {
                    int i=idx(x,y-dy); grid[i]=Particle.HEAT; age[i]=0; dirty[i]=true;
                    break;
                }
            }
        }
        spreadHeat(x,y);
    }

    // ── Семена ────────────────────────────────────────────
    private void updateSeed(int x, int y) {
        // Сначала падают как порошок
        if (canDisplace(x,y,x,y+1)) { swap(x,y,x,y+1); return; }

        // Проверяем условия роста: под нами земля, рядом вода
        int type = get(x,y);
        boolean onDirt = (get(x,y+1)==Particle.DIRT || get(x,y+1)==Particle.GRASS);
        boolean hasWater = hasNeighbor(x,y,Particle.WATER,2);

        if (onDirt && hasWater) {
            age[idx(x,y)]++;
            if (age[idx(x,y)] > 80 + rng.nextInt(40)) {
                replace(x, y, type==Particle.WHEAT_SEED ? Particle.WHEAT : Particle.POTATO);
            }
        }
    }

    // ── Пшеница (росток → реакция с жаром → мука) ────────
    private void updateWheat(int x, int y) {
        // Пшеница горит и даёт муку при нагреве
        if (Particle.isHot(get(x,y+1)) || Particle.isHot(get(x,y-1)) ||
            Particle.isHot(get(x-1,y)) || Particle.isHot(get(x+1,y))) {
            if (rng.nextInt(20)==0) replace(x, y, Particle.FLOUR);
        }
    }

    // ── Проверка реакций для статичных частиц ────────────
    private void checkReactions(int x, int y) {
        int type = get(x,y);
        if (type==Particle.DOUGH || type==Particle.DOUGH_Y) {
            // Тесто: проверяем нагрев рядом
            if (hasNeighborHot(x,y)) {
                age[idx(x,y)]++;
                if (age[idx(x,y)] > 60 + rng.nextInt(30)) {
                    replace(x, y, type==Particle.DOUGH_Y ? Particle.BREAD_PUFF : Particle.BREAD);
                }
            }
        }
        if (type==Particle.POTATO) {
            if (hasNeighborHot(x,y)) {
                age[idx(x,y)]++;
                if (age[idx(x,y)] > 50 + rng.nextInt(20)) {
                    // Была ли соль рядом?
                    boolean hadSalt = hasNeighbor(x,y,Particle.SALT,1);
                    replace(x, y, hadSalt ? Particle.TASTY_POTATO : Particle.FRIED_POTATO);
                }
            }
        }
    }

    // ── Смешивание частиц при попадании на одну клетку ───
    // Вызывается когда на клетку падает частица
    public void reactOnContact(int x, int y, int incoming) {
        int existing = get(x,y);
        // Мука + Вода = Тесто
        if ((existing==Particle.FLOUR && incoming==Particle.WATER) ||
            (existing==Particle.WATER && incoming==Particle.FLOUR)) {
            replace(x,y,Particle.DOUGH); return;
        }
        // Тесто + Дрожжи = Дрожжевое тесто
        if ((existing==Particle.DOUGH && incoming==Particle.YEAST) ||
            (existing==Particle.YEAST && incoming==Particle.DOUGH)) {
            replace(x,y,Particle.DOUGH_Y); return;
        }
        // Тесто + Соль = Тесто с солью (чуть другой DOUGH_Y цвет — упрощение)
        if ((existing==Particle.DOUGH_Y && incoming==Particle.SALT)) {
            replace(x,y,Particle.DOUGH_Y); return;
        }
    }

    // ── Проверка вытеснения ──────────────────────────────
    private boolean canDisplace(int fx, int fy, int tx, int ty) {
        if (!inBounds(tx,ty)) return false;
        int from = get(fx,fy);
        int to   = get(tx,ty);
        if (to == Particle.EMPTY) return true;
        // Жидкость вытесняет порошок снизу (нет, наоборот — порошок тонет в воде)
        if (Particle.isPowder(from) && Particle.isLiquid(to)) return true;
        // Газ поднимается сквозь жидкость
        if (Particle.isGas(from) && Particle.isLiquid(to)) return true;
        return false;
    }

    private boolean hasNeighbor(int x, int y, int type, int radius) {
        for (int dy=-radius; dy<=radius; dy++)
            for (int dx=-radius; dx<=radius; dx++)
                if (inBounds(x+dx,y+dy) && get(x+dx,y+dy)==type) return true;
        return false;
    }

    private boolean hasNeighborHot(int x, int y) {
        int[][] dirs={{0,-1},{0,1},{-1,0},{1,0}};
        for (int[] d:dirs) if (Particle.isHot(get(x+d[0],y+d[1]))) return true;
        return false;
    }

    // ── Рисование кистью ─────────────────────────────────
    public void paint(int cx, int cy, int type, int radius) {
        for (int dy=-radius; dy<=radius; dy++) {
            for (int dx=-radius; dx<=radius; dx++) {
                if (dx*dx+dy*dy > radius*radius) continue;
                int x=cx+dx, y=cy+dy;
                if (!inBounds(x,y)) continue;
                if (type==Particle.EMPTY) { replace(x,y,Particle.EMPTY); continue; }
                if (get(x,y)==Particle.EMPTY ||
                    (type!=Particle.WALL && type!=Particle.OVEN && type!=Particle.STOVE)) {
                    int prev = get(x,y);
                    replace(x,y,type);
                    // Проверяем реакцию с соседями
                    checkContactReactions(x,y,prev,type);
                }
            }
        }
    }

    private void checkContactReactions(int x, int y, int prev, int newType) {
        // Ищем соседей для реакций
        int[][] dirs={{0,-1},{0,1},{-1,0},{1,0}};
        for (int[] d:dirs) {
            int nx=x+d[0], ny=y+d[1];
            if (!inBounds(nx,ny)) continue;
            int n = get(nx,ny);
            reactOnContact(x,y,n);
            reactOnContact(nx,ny,newType);
        }
    }
}
