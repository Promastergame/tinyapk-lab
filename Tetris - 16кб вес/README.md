# Tetris

Compact Android Tetris implemented in pure Java with `SurfaceView` and `Canvas`. No Gradle, no Kotlin, no AndroidX, and no external game engine.

Компактный Android Tetris, реализованный на чистом Java через `SurfaceView` и `Canvas`. Без Gradle, без Kotlin, без AndroidX и без внешнего игрового движка.

## Screenshot / Скриншот

<p align="center">
  <img src="../docs/images/tetris-gameplay.jpg" alt="Tetris gameplay screenshot" width="320">
</p>

## Build Output / Артефакты сборки

- `build/Tetris.apk` - 16,811 bytes
- `build/Tetris-release.apk` - 16,811 bytes

## Gameplay Features / Особенности геймплея

- All 7 classic tetrominoes. / Все 7 классических тетромино.
- Ghost piece preview. / Призрачная проекция точки приземления.
- Simple wall kick logic on rotation. / Простая wall kick-логика при повороте.
- Score, line counter, and level progression. / Очки, счётчик линий и прогрессия по уровням.
- Falling speed increases with level. / Скорость падения растёт с уровнем.
- Next-piece preview panel. / Панель со следующей фигурой.
- Tap-to-restart flow after game over. / Перезапуск по тапу после окончания партии.

## Controls / Управление

| Input / Ввод | Action / Действие |
| --- | --- |
| Tap / Тап | Rotate piece / Повернуть фигуру |
| Swipe left / right / Свайп влево / вправо | Move piece / Сдвинуть фигуру |
| Swipe down / Свайп вниз | Hard drop / Мгновенно сбросить |
| Swipe up / Свайп вверх | Rotate piece / Повернуть фигуру |
| Tap after game over / Тап после поражения | Restart game / Перезапустить игру |

## Technical Notes / Технические заметки

The project is intentionally split into two layers: `TetrisGame.java` contains the pure game rules and board state, while `TetrisView.java` handles rendering, animation timing, and touch input.

Проект намеренно разделён на два слоя: `TetrisGame.java` содержит чистые игровые правила и состояние поля, а `TetrisView.java` отвечает за рендеринг, тайминг анимации и сенсорный ввод.

## Rendering / Рендеринг

The UI uses a dark gradient background, highlighted cells, a ghost piece outline, and a compact side panel for score, lines, level, and next-piece preview.

Интерфейс использует тёмный градиентный фон, подсвеченные клетки, контур ghost piece и компактную боковую панель для очков, линий, уровня и следующей фигуры.

## Build Notes / Заметки по сборке

The build pipeline is fully manual: `aapt2 -> ecj -> d8/R8 -> zipalign -> apksigner`. The game is designed as a kilobyte-scale reference project for minimal Android builds.

Сборочная цепочка полностью ручная: `aapt2 -> ecj -> d8/R8 -> zipalign -> apksigner`. Игра задумана как эталонный проект для минималистичной Android-сборки в килобайтном масштабе.

- [Manual build notes](../%D0%B1%D0%B8%D0%BB%D0%B4%20%D0%B0%D0%BF%D0%BA/README.md)
- [R8 / ProGuard guide](../PROGUARD_README.md)
