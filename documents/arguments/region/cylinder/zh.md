# `cyl()` 和 `hcyl()`

`cyl()` 表示一个圆柱体的[区域](../zh.md)，该圆柱体的底面与 x-z 平面平行，高与 y 轴平行。`hcyl()` 表示空心的圆柱体。

为了使得在放置方块时的高度正确，圆柱体的高采取“前闭后开区间”的原则。如果某个精确坐标正好在圆柱体的底部，则该坐标被认为属于该区域，但如果正好在圆柱体的顶部，则该坐标被认为不属于该区域。

## 语法

- `cyl(<半径>, [高度], [中心坐标])`
- `hcyl(<半径>, [高度], [中心坐标], [边框类型])`

其中，`hcyl(...)` 在效果上等价于 `outline(cylinder(...))`。

实体的圆柱体可以表述为这样的集合：`{(x, y, z)|distance((center.x, center.y), (x, y)) <= radius && center.y - height / 2 <= y < center.y + height / 2}`。

## 参数

### `<半径>`

双精度浮点数。圆柱体的半径，不能小于 0。

### `[高度]`

双精度浮点娄。圆柱体的高度，不能小于 0。如果未指定，则默认为 1。

### `[中心坐标]`

[坐标](../../pos/zh.md)。如果未指定为浮点数，则表示为方块坐标的正中心。如果未指定，则为执行背景所在的方块坐标的正中心位置。

### `[边框类型]`

空心圆柱体的边框类型。可接受的值包括：`outline`、`outline_connected`、`wall`、`wall_connected`、`floor_and_ceil`。

## 示例

- `cyl(3)`：以执行背景所在的方块坐标的中心点为中心，半径为 3，高度为 1 的圆柱。
- `cyl(3, 2)`：以执行背景所在的方块坐标的中心点为中心，半径为 3，高度为 2 的圆柱。
- `cyl(3, 2, 2 5 10)`：以 (2.5 5.5 10.5)（即方块坐标 (2 5 10) 的中心位置）为中心，半径为 3，高度为 2 的圆柱。
- `cyl(3, 2, 2.0 5.2 10.3)`：以精确坐标 (2.0 5.2 10.3) 为中心，半径为 3，高度为 2 的圆柱。
- `hcyl(5, 8)`：以执行背景所在的方块坐标的中心点为中心，半径为 5、高度为 8 的空心圆柱。