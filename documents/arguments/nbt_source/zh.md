# NBT 来源：指定从哪里获取 NBT

**NBT 来源**是一种[参数类型](../zh.md)，表示从哪里获取 NBT 数据。注意：NBT 来源是一整个 NBT，通常只会是复合标签（compound）。实际运行命令时，通常还需要指定路径和[聚合类型](../nbt_concentration_type/zh.md)。

## 语法

- `block <区域>`：指定区域（可以是单个坐标）内的所有方块实体的 NBT 数据。如果该区域内的所有方块都不是方块实体，则会失败。
- `entity <实体选择器>`：指定实体的 NBT 数据。选择器可以选择多个实体。
- `literal <NBT 函数>`：直接指定 NBT 数据，NBT 函数在使用时，会以 null 为参数。
- `storage <id>`：指定存储位置的 NBT 数据。与原版的 `/data ... storage <id>` 等同。

关于参数用法，请参见[区域](/documents/arguments/region/zh.md)、[实体选择器](/documents/arguments/entity_selector/zh.md)、[NBT 函数](../nbt_function/zh.md)。

## 示例

- `block 5 1 4`
- `block sphere(5)`
- `entity @e[type=cow,limit=1]`
- `entity @p[c=-5]`
- `storage namespace:id`

## 参见

- [NBT 目标](../nbt_target/zh.md)
- [`/nbt` 命令](/documents/commands/nbt/zh.md)