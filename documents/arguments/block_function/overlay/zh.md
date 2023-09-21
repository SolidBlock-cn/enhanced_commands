# `overlay()`

此[方块函数](../zh.md)用于依次应用多个方块函数。一个方块函数的计算结果会作为下一个方块函数当前方块以用于计算。

## 语法

`overlay([方块函数], ...)`

可以没有方块函数，表示不执行任何操作。

## 示例

- `overlay()`：不执行任何操作。
- `overlay(*, dry())`：随机方块，并去除其中的水。相当于 `**dry()` 或 `dry(*)`。
- `overlay(#stairs, [*], [waterlogged=false])`：随机选择一个楼梯方块，然后添加随机的属性，并添加 `waterlogged=false` 属性。相当于 `#stairs*[*]*[waterlogged=false]`，实际上等效于 `#stairs[*, waterlogged=false]`。