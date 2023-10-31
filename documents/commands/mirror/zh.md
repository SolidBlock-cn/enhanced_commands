# `/mirror` 和 `//mirror`

此[命令](../zh.md)用于镜像（或者说是翻转）一个区域内的方块和实体。只支持绕 x、y 或 z 轴翻转。

## 语法

- `/mirror <区域> [轴] [关键字参数]`
- `//mirror [轴] [关键字参数]`

## 参数

### `<区域>`

需要影响的[区域](/documents/arguments/region/zh.md)。对于 `//mirror`，不指定此参数，使用玩家的活动区域。

### `[轴]`

镜像所使用的轴。

### 关键字参数

**此命令支持 [`/fill`](../fill/zh.md) 和 [`/move`](../move/zh.md) 中的所有关键字参数。**

除了 `/fill` 和 `/move` 中的关键字参数外，`/mirror` 和 `//mirror` 还支持以下关键字参数：

- `pivot`：镜像时所影响围绕的点。必须是[方块坐标](/documents/arguments/pos/zh.md)。如果未指定，则使用执行源所在的方块坐标。

## 示例

- `/mirror sphere(5)`：沿执行源的朝向的前后方向，镜像半径为 5 的球体的区域。
- `//mirror`：沿执行源的朝向的前后方向，镜像玩家的活动区域。
- `//mirror left_right select=true`：沿执行源的朝向的左右方向，镜像玩家的活动区域，并将活动区域设置为镜像后的区域。
- `/mirror cuboid(1 1 1, 5 5 5) pivot=7 7 7`：将 (1, 1, 1) 和 (5, 5, 5) 之间的长方体区域，按 (7, 7, 7) 沿前后方向进行镜像。
- `//mirror front_back transform_only=!air`：镜像玩家的活动区域内的除空气以外的方块。
- `//mirror y affect_entities=@e`：沿 y 轴方向，镜像活动区域内的方块实体。
- `/mirror sphere(10) x affect_entities=@e transform_only=!*`：镜像半径为 10 的球体区域内的所有实体，不影响任何方块。