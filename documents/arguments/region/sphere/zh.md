# `sphere()`

球体的[区域](../zh.md)。

## 语法

`sphere(<半径>, [中心])`

## 参数

### `<半径>`

球的半径，可以是小数。负值有效，但是没有意义。

### `[中心]`

球的中心[坐标](../../pos/zh.md)，默认为执行背景所在方块坐标的中心点位置。

## 示例

- `sphere(5)`：以执行背景所在方块坐标中心位置为中心、半径为 5 个方块的圆。
- `sphere(5, ~~~0.0)`：以执行背景所在的精确位置为中心、半径为 5 个方块的圆。
- `sphere(5.5, 1 2 3)`：以坐标 (1, 2, 3) 的中心位置为中心、半径为 5.5 个方块的圆。
- `sphere(6.5, 1.0 2.0 3.0)`：以精确坐标 (1.0, 2.0, 3.0)（即方块坐标的西北角底部位置）为中心、半径为 6.5 个方块的圆。