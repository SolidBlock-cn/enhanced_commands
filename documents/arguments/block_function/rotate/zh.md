# `rotate()`：旋转方块

此[方块函数](../zh.md)可对当前方块进行旋转。

旋转只对方块自身有效，而不是旋转一整个区域，也就是说这一函数并不会修改各个方块的位置。

> 如果想旋转一个区域，可使用 [`/rotateblocks` 命令](/documents/commands/rotateblocks/zh.md)。

## 语法

`rotate(<旋转方向>|*)`

## 示例

- `rotate(none)`：不进行旋转，不操作。
- `rotate(clockwise_90)`：顺时针旋转 90 度。
- `rotate(180)`：顺时针旋转 180 度。
- `rotate(counterclockwise_90)`：逆时针旋转 90 度。
- `rotate(*)`：往随机方向旋转。