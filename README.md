# Cozy Lamp (Fabric 1.21.1)

Настольная лампа с тёплым светом.

## Управление
- ПКМ — включить / выключить
- Shift + ПКМ (пустые руки) — яркость: 3 уровня (свет 7 / 11 / 14)
- Редстоун-сигнал включает лампу, пропадание сигнала выключает
- Крафт: жёлтая шерсть, факел, медный слиток (в столбик, сверху вниз)

## Сборка
1. Нужна Java 21.
2. Самый надёжный путь: сгенерируй пустой шаблон на https://fabricmc.net/develop/template/
   (Minecraft 1.21.1, Mojang mappings, без data generation) и скопируй в него папку `src` из этого архива,
   заменив `fabric.mod.json`.
3. Либо в этой папке: `gradle wrapper --gradle-version 8.10.2`, затем `./gradlew build`.
4. Готовый мод: `build/libs/cozylamp-1.0.0.jar` -> в папку mods вместе с Fabric API.

Если Gradle не находит версию Fabric API, возьми актуальную для 1.21.1 на modrinth.com/mod/fabric-api
и поправь `fabric_version` в gradle.properties.

## Как заменить звук щелчка
Положи свой .ogg в assets/cozylamp/sounds/ и поправь assets/cozylamp/sounds.json.
