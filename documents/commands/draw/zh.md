# `/draw`

此[命令](../zh.md)用于在世界内绘制一条曲线。

绘制曲线的过程是，沿着这条曲线隔着指定的间隔迭代一些点，并在这些点的位置放置方块。绘制曲线时，可以指定半径，那么这些点的指定半径的范围内的方块都有可能受到影响。

## 语法

`/draw <曲线> <方块> [关键字参数：(同 fill) | interval | thickness]`

关键字参数继承 [`/fill`](../fill/zh.md) 的所有关键字参数，外加 `interval` 和 `thickness`。

## 参数

### `<曲线>`

需要绘制的[曲线](/documents/arguments/curve/zh.md)。

### `<方块>`

需要在此曲线上使用的[方块函数](/documents/arguments/block_function)。

### 关键字参数

#### `interval`

双精度浮点数。曲线上的采点间隔。默认为 0（相当于 0.05），即绘制一条基本连续的曲线。

#### `thickness`

双精度浮点数。曲线粗细，默认为 0。如果粗细不为 0，那么每个点上都会绘制圆形。

## 示例

- `/curve straight(~~~, ~5~6~7) stone`：在当前位置和相对当前位置的 (5,6,7) 的位置之间使用石头绘制一条直线。