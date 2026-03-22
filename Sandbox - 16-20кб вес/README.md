# Sandbox / Песочница

Minimal falling-sand sandbox with particles, heat, plant growth, and simple cooking interactions. Built in pure Java without Gradle, AndroidX, or third-party game frameworks.

Минималистичная falling-sand-песочница с частицами, жаром, ростом растений и простыми кулинарными реакциями. Сделана на чистом Java без Gradle, AndroidX и сторонних игровых фреймворков.

## Screenshot / Скриншот

<p align="center">
  <img src="../docs/images/sandbox-gameplay.jpg" alt="Sandbox gameplay screenshot" width="320">
</p>

## Build Output / Артефакты сборки

- `build/Sandbox.apk` - 20,907 bytes
- `build/Sandbox-release.apk` - 16,811 bytes

## Core Systems / Основные системы

| System / Система | Description / Описание |
| --- | --- |
| Powders / Порошки | Sand, flour, salt, yeast, and ash fall, pile up, and react with liquids or heat.<br>Песок, мука, соль, дрожжи и пепел осыпаются, накапливаются и реагируют с жидкостями или жаром. |
| Fluids / Жидкости | Water flows, spreads horizontally, fills gaps, and can turn into steam.<br>Вода течёт, растекается по горизонтали, заполняет пустоты и может превращаться в пар. |
| Heat / Жар | Fire, heat cells, stoves, and ovens drive transformations across nearby materials.<br>Огонь, клетки жара, плиты и духовки запускают превращения соседних материалов. |
| Growth / Рост | Wheat and potato seeds can grow when placed on suitable ground near water.<br>Семена пшеницы и картошки могут расти на подходящей почве рядом с водой. |
| Cooking / Готовка | Dough can bake into bread, potatoes can fry, and several ingredient combinations create new states.<br>Тесто может выпекаться в хлеб, картошка может жариться, а комбинации ингредиентов создают новые состояния. |

## Materials / Материалы

| Material / Материал | Behavior / Поведение |
| --- | --- |
| Sand / Песок | Falls downward and settles diagonally when needed. |
| Water / Вода | Flows and fills open space. |
| Dirt / Земля | Solid support block. |
| Grass / Дёрн | Solid surface suitable for planting. |
| Fire / Огонь | Rises upward and spreads heat. |
| Flour / Мука | Powder ingredient used for dough. |
| Salt / Соль | Powder ingredient that improves potato recipes. |
| Yeast / Дрожжи | Powder ingredient used for risen dough. |
| Wheat Seed / Семя пшеницы | Grows into wheat with water and proper soil. |
| Potato Seed / Семя картошки | Grows into potato with water and proper soil. |
| Pan / Сковородка | Static cooking surface. |
| Stove / Плита | Constant source of fire and heat. |
| Oven / Духовка | Heats the inner vertical space above it. |

## Recipes / Рецепты

| Recipe / Рецепт | Result / Результат |
| --- | --- |
| Flour + Water / Мука + Вода | Dough / Тесто |
| Dough + Yeast / Тесто + Дрожжи | Yeast dough / Дрожжевое тесто |
| Dough + Oven / Тесто + Духовка | Bread / Хлеб |
| Yeast dough + Oven / Дрожжевое тесто + Духовка | Puffy bread / Пышный хлеб |
| Potato + Stove or Pan / Картошка + Плита или Сковородка | Fried potato / Жареная картошка |
| Potato + Salt + Stove / Картошка + Соль + Плита | Tasty potato / Вкусная картошка |
| Wheat + Heat / Пшеница + Жар | Flour / Мука |

## Controls / Управление

- Tap or drag to paint the selected material. / Тапайте или тяните пальцем, чтобы рисовать выбранным материалом.
- Use `-` and `+` to change brush radius. / Используйте `-` и `+`, чтобы менять радиус кисти.
- Use `Clear` to reset the world. / Используйте `Clear`, чтобы сбросить мир.

## Architecture / Архитектура

The simulation is split between a pure-logic world model and a rendering layer. `SandWorld.java` handles particle state, movement, reactions, growth, and heat propagation, while `SandView.java` is responsible for drawing and touch input.

Симуляция разделена на модель мира и слой рендеринга. `SandWorld.java` отвечает за состояние частиц, движение, реакции, рост и распространение жара, а `SandView.java` отвечает за отрисовку и сенсорный ввод.

## Build Notes / Заметки по сборке

The project follows the same minimal pipeline used across this repository: `aapt2 -> ecj -> d8/R8 -> zipalign -> apksigner`. For step-by-step build guidance, see the repository-level documentation.

Проект использует ту же минималистичную сборочную цепочку, что и весь репозиторий: `aapt2 -> ecj -> d8/R8 -> zipalign -> apksigner`. Пошаговую инструкцию по сборке смотрите в документации репозитория.

- [Manual build notes](../%D0%B1%D0%B8%D0%BB%D0%B4%20%D0%B0%D0%BF%D0%BA/README.md)
- [R8 / ProGuard guide](../PROGUARD_README.md)
