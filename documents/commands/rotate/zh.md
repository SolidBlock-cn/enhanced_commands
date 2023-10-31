# `/rotate` 和 `//rotate`

此[命令](../zh.md)用于绕一个轴逆时针旋转一个区域内的方块和实体。

## 语法

- `/rotate <区域> <角度> [关键字参数]`
- `/rotate <区域> <角度> [around <方向> [关键字参数]]`
- `/rotate <区域> <角度> [around vector <x> <y> <z> [关键字参数]]`
- `//rotate <角度> [关键字参数]`
- `//rotate <角度> [around <方向> [关键字参数]]`
- `//rotate <角度> [around vector <x> <y> <z> [关键字参数]]`

## 参数

### `<区域>`

需要移动的[区域](/documents/arguments/region/zh.md)。使用 `//rotate` 时不指定区域，使用玩家的活动区域。

### `<角度>`

旋转的角度。非零值须以 `deg`、`rad` 或 `turn` 为单位。

### `<方向>`

旋转所需要围绕的轴的[方向](/documents/arguments/direction/zh.md)。如未指定，则默认为 y 轴正方向，即向上。

### `<x> <y> <z>`

旋转所需要围绕的轴的向量。可以是小数。

### 关键字参数

**此命令支持 [`/fill`](../fill/zh.md) 和 [`/move`](../move/zh.md) 中的所有关键字参数。**

除了 `/fill` 和 `/move` 中的关键字参数外，`/rotate` 和 `//rotate` 还支持以下关键字参数：

- `interpolate`：布尔值，表示旋转时是否进行插值。对于围绕坐标轴且旋转角度为 90deg 的倍数的旋转，通常不需要插值。
- `pivot`：[坐标](/documents/arguments/pos/zh.md)，旋转的中心点。必须是方块坐标。如果未指定，则默认为执行源所在的坐标。

## 示例

- `/rotate sphere(5) 30deg`：将以执行源所在坐标为中心、半径为 5 个方块的球体区域内的方块逆时针旋转 30 度。
- `//rotate 30deg`：将活动区域内的方块逆时针旋转 30 度。
- `//rotate 30deg affect_entities=@e`：将活动区域内的方块和实体逆时针旋转 30 度。
- `//rotate 30deg interpolate=true`：将活动区域内的方块逆时针旋转 30 度，并进行插值。
- `/rotate sphere(5, ^^^3) 30deg`：将以执行源所在坐标前方 3 个方块的位置为中心、半径为 5 个方块的球体区域内的方块绕执行源所在坐标逆时针旋转 30 度。
- `/rotate sphere(5, ^^^3) 30deg pivot=^^^3`：将以执行源所在坐标前方 3 个方块的位置为中心、半径为 5 个方块的球体区域内的方块绕球体中心逆时针旋转 30 度。
- `//rotate 0.5turn around east`：将活动区域内的方块绕向东方向逆时针旋转半个圆周。
- `//rotate 0.25turn around vector 1 2 3`：将活动区域内的方块绕向量 (1, 2, 3) 逆时针旋转 0.25 个圆周。
- `//rotate 1rad around vector 1 2 3 keep_remaining=true`：将活动区域内的方块绕向量 (1, 2, 3) 逆时针旋转 1 个弧度，并保留原有的位置的方块。