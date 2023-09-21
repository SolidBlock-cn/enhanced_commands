# `mirror()`

此[方块函数](../zh.md)会对方块进行镜像操作，其效果相当于在结构方块中进行的镜像操作。

## 语法

`mirror(<镜像方向>|*|forward|side)`

## 参数

- `<镜像方向>`：固定的镜像方向，可用的值有 `front_back` 和 `left_right`。
- `*`：表示随机方向的镜像（包括没有操作的镜像）。
- `front`：按照执行时的水平朝向的前后方向镜像。
- `side`：按照执行时的水平朝向的左右方向镜像。

注意：不支持上下镜像。

## 示例

- `mirror(front_back)`：沿 X 方向镜像。
- `mirror(left_right)`：沿 Z 方向镜像。
- `mirror(none)`：不进行镜像。
- `mirror(*)`：沿 X 或 Z 的随机方向镜像。
- `mirror(front)`：沿执行时的水平朝向的前后方向镜像。当执行朝向靠近 X 轴时，相当于 `mirror(front_back)`，靠近 Z 轴时，相当于 `mirror(left_right)`。