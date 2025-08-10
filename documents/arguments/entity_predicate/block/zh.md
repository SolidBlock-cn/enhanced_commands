# `block`：检测实体所在位置的方块

此[实体谓词](../zh.md)可用于测试实体所在位置为基础的任意坐标（包括相对坐标和局部坐标）的方块，可以单独指定一个[方块谓词](/documents/arguments/block_predicate/zh.md)，也可以指定由[坐标](/documents/arguments/pos/zh.md)和[方块谓词](/documents/arguments/block_predicate/zh.md)的多个配对，用于检测指定坐标的方块。

## 语法

- `[block=<方块谓词>]`：指定单个方块谓词，检测实体所在位置的方块。
- `[block={<坐标 1> = <方块谓词 1>, <坐标 2> = <方块谓词 2>, ...}]`：指定多个坐标和方块谓词，检测指定位置的方块。

> 注意：如果方块谓词是 [NBT 谓词](/documents/arguments/block_predicate/nbt/zh.md)，请注意给 NBT 谓词加上括号或者星号，以免被解析为第二种语法。例如，`[block={Inventory: []}]` 是错误的，应当写成 `[block=({Inventory: []})]` 或 `[block=*{Inventory: []}]`。

## 示例

- `@e[block=air]`：选择所在位置为空气的实体。
- `@e[block=water|*[waterlogged=true]]`：选择所在位置为水或任意含水方块的实体。
- `@a[block={~~-1~=redstone_block}]`：选择下方一格位置为红石块的玩家。
- `@e[block={~~-1~=grass_block, ~~-2~=dirt}]`：选择下方一格位置为草方块，且下方两格位置为泥土的玩家。

## 数据结构

当直接指定单个方块谓词时：

- `type`：此时为 `"enhanced_commands:block_predicate"`。
- `predicate`：[方块谓词](/documents/arguments/block_predicate/zh.md)。

当指定坐标与方块谓词时：

- `type`：此时为 `"enhanced_commands:block_predicates"`。
- `predicates`：列表。
    - 列表中的一个元素。
        - `pos`：[坐标](/documents/arguments/pos/zh.md)。
        - `block`：[方块谓词](/documents/arguments/block_predicate/zh.md)。