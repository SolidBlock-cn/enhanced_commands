# NBT 源

**NBT 源**是一种[参数类型](../zh.md)，表示从哪里获取 NBT 数据，以及当获取到多个 NBT 数据时应该如何处理。注意：NBT 源获取的是一整个 NBT，且这个 NBT 一定是复合标签（compound）。

## 语法

- `block <方块坐标>`：指定坐标的方块的 NBT 数据。如果该方块不是方块实体，则会失败。
- `blocks <区域> <聚合类型>`：指定区域内的所有方块实体的 NBT 数据。如果该区域内的所有方块都不是方块实体，则会失败。
- `entity <实体选择器>`：指定实体的 NBT 数据。选择器必须仅选择一个实体，否则语法无效。
- `entities <实体选择器> <聚合类型>`：指定实体的 NBT 数据。选择器可以选择多个实体。
- `storage <id>`：指定存储位置的 NBT 数据。

关于参数用法，请参见[坐标](/documents/arguments/pos/zh.md)、[区域](/documents/arguments/region/zh.md)、[聚合类型](/documents/arguments/concentration_type/zh.md)、[实体选择器](/documents/arguments/entity_selector/zh.md)。

## 示例

- `block 5 1 4`
- `blocks sphere(5) average`
- `entity @e[type=cow,limit=1]`
- `entities @p[c=-5] list`
- `storage namespace:id`

## 参见

- [NBT 目标](../nbt_target/zh.md)