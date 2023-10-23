# `outwards()`

由一个中心坐标向外延伸特定距离形成的长方体的[区域](../zh.md)。

类似于 [`cuboid()`](../cuboid/zh.md)，但是迭代顺序是从中间向外迭代。

## 语法

`outwards(<中心坐标>, [x] [y] [z])`

## 参数

### `<中心坐标>`

[方块坐标](/documents/arguments/pos/zh.md)，区域的中心的位置，将从中心点开始向外迭代。

### `[x] [y] [z]`

在特定轴上向外延伸的量。当 z 未指定时，默认等于 z；当 y 未指定时，默认等于 x；当 x 未指定时，默认等于 0。

区域的体积为 (1 + 2<var>x</var>)(1 + 2<var>y</var>)(1 + 2<var>z</var>)。

## 示例

- `outwards(~~~)`：执行环境所在的方式坐标，仅这一个坐标。等价于 `outwards(~~~, 0)`。
- `outwards(~~~, 1)`：以当前坐标为中心，各轴向外延伸 1 格的正方体区域，体积为 3<sup>3</sup> = 27。等价于 `outwards(~~~, 1 1 1)`。
- `outwards(~~~, 1 2)`：以当前坐标为中心，水平方向向外延伸 1 格、竖直方向向外延伸 2 格的长方体区域。等价于 `outwards(~~~, 1 2 1)`。
- `outwards(~~~, 1 2 3)`：以当前坐标为中心，x 方向延伸 1 格、y 方向延伸 2 格、z 方向延伸 3 格的 3 × 5 × 7 的区域。