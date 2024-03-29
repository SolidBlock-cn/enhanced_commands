# `/testfor block`

此[命令](../../zh.md)用于获取世界内指定位置的方块的数据，同时可以检测此方块是否符合指定的谓词，检测过程中会对检测的结果进行详细描述。

检测过程中生成描述性文本会比较复杂，因此如需在命令方块或数据包中使用，建议直接使用 `/if block`。

## 语法

`/testfor block <坐标> [方块谓词] [关键字参数：force_load]`

如果没有提供方块谓词，则会提供该方块的一些基本信息，包括名称、id 和属性等。

如果提供了方块谓词，则会检测该方块是否符合此方块谓词，并显示计算过程。

## 参数

### `<坐标>`

需要检测的[方块坐标](/documents/arguments/pos/zh.md)。

坐标可以超出世界的界限以及建筑限制。建筑限制外的方块通常是虚空空气。

### `[方块谓词]`

[方块谓词](/documents/arguments/block_predicate/zh.md)。

### 关键字参数：`force_load`

布尔值，默认为 `false`。

如果为 `false`，当要测试的坐标位于未加载的区块内时，会抛出错误。

如果为 `true`，当要测试的坐标位于未加载的区块内时，会强制加载此区块。

## 示例

- `/testfor block ~~-1~`：检测命令执行源的位置下方一格的方块。
- `/testfor block ~ -165 ~`：检测 y 轴为 -165 的位置的方块。尽管此坐标超出了建筑限制范围，但仍能检测方块。
- `/testfor block ~ ~ ~10000`：命令执行可能会失败，因为这个坐标通常没有被加载。
- `/testfor block ~~-1~ redstone_block`：检测命令执行源位置下方一格的方块是否为红石块。
- `/testfor block ~~-1~ #planks`：检测命令执行源位置下方一格的方块是否拥有 `#planks` 标签。
- `/testfor block ~ ~ ~10000 air`：命令执行可能会失败，因为这个坐标通常没有被加载。
- `/testfor block ~ ~ ~10000 air force_load=true`：检测命令执行源位置 z 轴增加 10000 个方块后的位置的方块是否为空气，会强制加载这个位置。

## 参见

- [`/testfor`](../zh.md)