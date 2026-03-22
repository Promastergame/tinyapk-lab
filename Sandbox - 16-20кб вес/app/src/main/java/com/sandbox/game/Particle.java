package com.sandbox.game;

public class Particle {

    // Типы частиц
    public static final int EMPTY   = 0;
    public static final int SAND    = 1;
    public static final int WATER   = 2;
    public static final int DIRT    = 3;  // земля
    public static final int GRASS   = 4;  // дёрн
    public static final int WHEAT_SEED = 5;  // семя пшеницы
    public static final int POTATO_SEED = 6; // семя картошки
    public static final int FLOUR   = 7;  // мука
    public static final int SALT    = 8;  // соль
    public static final int YEAST   = 9;  // дрожжи
    public static final int FIRE    = 10; // огонь
    public static final int ASH     = 11; // пепел
    public static final int WHEAT   = 12; // пшеница (росток)
    public static final int POTATO  = 13; // картошка (вырощенная)
    public static final int DOUGH   = 14; // тесто
    public static final int DOUGH_Y = 15; // дрожжевое тесто
    public static final int BREAD   = 16; // хлеб
    public static final int BREAD_PUFF = 17; // пышный хлеб
    public static final int FRIED_POTATO = 18; // жареная картошка
    public static final int TASTY_POTATO = 19; // вкусная картошка с солью
    public static final int STEAM   = 20; // пар
    public static final int WALL    = 21; // стена/инструмент — сковородка
    public static final int OVEN    = 22; // духовка
    public static final int STOVE   = 23; // плита
    public static final int HEAT    = 24; // жар (невидимый, от плиты/духовки)

    public static final int TYPE_COUNT = 25;

    // Цвета (ARGB)
    public static final int[] COLORS = {
        0x00000000, // EMPTY
        0xFFF4D03F, // SAND — жёлтый
        0xFF2980B9, // WATER — синий
        0xFF8B6914, // DIRT — коричневый
        0xFF5D8A3C, // GRASS — зелёный
        0xFFD4AC7A, // WHEAT_SEED — светло-коричневый
        0xFF9B7B3A, // POTATO_SEED — темнее
        0xFFF5CBA7, // FLOUR — кремовый
        0xFFE8DAEF, // SALT — белесый
        0xFFFDFEFE, // YEAST — белый
        0xFFFF6B35, // FIRE — оранжевый
        0xFF7F8C8D, // ASH — серый
        0xFF27AE60, // WHEAT — яркий зелёный
        0xFFE67E22, // POTATO — оранжево-коричневый
        0xFFF0D0A0, // DOUGH — тесто
        0xFFE8C890, // DOUGH_Y — чуть темнее
        0xFFD4A055, // BREAD — золотой
        0xFFE8B870, // BREAD_PUFF — пышный светлее
        0xFFD4820A, // FRIED_POTATO — поджаренный
        0xFFCC7700, // TASTY_POTATO — с солью
        0xAADDDDFF, // STEAM — полупрозрачный
        0xFF888888, // WALL/PAN — серый металл
        0xFF444444, // OVEN — тёмный
        0xFF222222, // STOVE — почти чёрный
        0x33FF4400, // HEAT — красноватый прозрачный
    };

    // Имена для UI
    public static final String[] NAMES = {
        "Пусто", "Песок", "Вода", "Земля", "Дёрн",
        "Семя пшеницы", "Семя картошки", "Мука", "Соль", "Дрожжи",
        "Огонь", "Пепел", "Пшеница", "Картошка",
        "Тесто", "Дрожж. тесто", "Хлеб", "Пышный хлеб",
        "Жар. картошка", "Вкусная картошка",
        "Пар", "Сковородка", "Духовка", "Плита", "Жар"
    };

    // Поведение
    public static boolean isPowder(int t) {
        return t==SAND||t==FLOUR||t==SALT||t==YEAST||t==ASH||t==WHEAT_SEED||t==POTATO_SEED;
    }
    public static boolean isLiquid(int t)  { return t==WATER; }
    public static boolean isGas(int t)     { return t==FIRE||t==STEAM||t==HEAT; }
    public static boolean isSolid(int t)   { return t==DIRT||t==GRASS||t==WALL||t==OVEN||t==STOVE||t==BREAD||t==BREAD_PUFF||t==WHEAT||t==POTATO||t==DOUGH||t==DOUGH_Y||t==FRIED_POTATO||t==TASTY_POTATO; }
    public static boolean isMovable(int t) { return isPowder(t)||isLiquid(t)||isGas(t); }
    public static boolean isHot(int t)     { return t==FIRE||t==HEAT||t==STOVE; }

    // Инструменты для рисования (показываем в UI)
    public static final int[] TOOLS = {
        SAND, WATER, DIRT, GRASS,
        WHEAT_SEED, POTATO_SEED, FLOUR, SALT, YEAST,
        FIRE, WALL, OVEN, STOVE, EMPTY
    };
}
