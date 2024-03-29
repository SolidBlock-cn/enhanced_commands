# `/wall` 和 `//wall`

此[命令](../zh.md)用于填充区域水平边缘的方块，从而产生区域内四周的围墙。

## 语法

- `/wall <区域> <方块> [关键字参数]`
- `//wall <方块> [关键字参数]`

上述命令等价于 `/outline <区域> <方块> wall [关键字参数]` 和 `//outline <区域> <方块> wall [关键字参数]`。

## 参数

此命令的所有参数均等价于 `/outline`，请参见 [`outline()`#参数](../outline/zh.md#参数)。

## 示例

- `/wall cuboid(1 1 1, 4 4 4) bricks`：使用砖块制作 (1, 1, 1) 到 (4, 4, 4) 之间的长方体的围墙。
- `//wall yellow_wool inner=water`：使用黄色羊毛制作活动区域的围墙，并在围墙内填充满水。