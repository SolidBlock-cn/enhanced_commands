# `/testfor biome`

此[命令](../../zh.md)用于对生物群系进行测试。

## 语法

- `/testfor biome [坐标]`：测试指定[坐标](/documents/arguments/pos/zh.md)处的生物群系。如果坐标未指定，则为命令执行位置的坐标。
- `/testfor biome <坐标> <生物群系>`：测试指定坐标处是否符合指定的生物群系。支持多选。支持使用标签。

## 示例

- `/testfor biome`
- `/testfor biome ~~5~`
- `/testfor biome ~~~ plains`
- `/testfor biome ~~~ forest|flower_forest`
- `/testfor biome ~ 0 ~ !desert|badlands`
- `/testfor biome ~~~ !!allows_surface_slime_spawns`