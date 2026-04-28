# `pick()`：随机选择物品函数

此[物品函数](../zh.md)用于从多个物品函数中随机选择。可以为每个随机项设置权重，如果不设置，那么其权重均为 1。

## 语法

- `pick(<物品函数> [权重], ...)`

## 参数

### `<物品函数>`

至少一个需要应用的[物品函数](../zh.md)。

### `[权重]`

浮点数，0 到 1 之间。每个物品函数可能被选中的相对概率。权重越大的物品函数越有可能被选中。如果省略，则默认为 1。可以为 0，但所有物品函数的权重之和不能为 0。

## 示例

- `pick(white_dye, blue_dye)`：随机选择白色染料或蓝色染料，其概率相等。相当于 `white_dye|black_dye`。
- `pick(white_dye 3, blue_dye 2)`：随机选择白色染料和蓝色染料，其白色染料的概率为 60%，蓝色染料的概率为 40%。
- `pick(white_dye 0, blue_dye 0)`：无效的物品函数，因为各项的权重之和为 0。

## 数据结构

- `type`：此时为 `enhanced_commands:pick`。
- `functions`：复合标签。随机选择的物品函数的列表。
    - `weighted`：布尔值。
    - `elements`：列表，仅当 `weighted` 为 false 时存在。
        - 物品函数，一个被随机抽取的项。
    - `entries`：列表，仅当 `weighted` 为 true 时存在。
        - 复合标签。一个项。
            - `element`：物品函数。
            - `weighted`：双精度浮点数。

## 参见

- [`pick()` 方块函数](../../block_function/pick/zh.md)
- [`pick()` NBT 函数](../../nbt_function/pick/zh.md)