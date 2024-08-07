# `checkerboard()`

此[方块函数](../zh.md)生成一个棋盘格的图案。可以指定棋盘格的大小和坐标运算格式。

一个棋盘格可以指定 1 个或多个方块函数，每个指定的方块函数可以单独指定权重。但是，指定 1 个方块函数的棋盘格虽然有效，通常没有什么意义。

棋盘格有三个参数：向下取整（`floor`）、比例（`scale`）和偏移（`offset`），都是三元向量，其效果分别如下：

- 向下取整：默认为 `(0, 0, 0)`。如果设为非零，那么非零的那个分量会作为除数，除坐标并向下取整，从而使棋盘格图案的棋盘变大。
- 比例：默认为 `(1, 1, 1)`。对于零值，该坐标轴上将不会有棋盘格图案，但是不影响棋盘格。对于非零值，该坐标轴上的棋盘格图案会变大，并可能使图案接近与格纹。
- 偏移：默认为 `(0, 0, 0)`，使整个棋盘格图案沿着该坐标偏移。

对于坐标为 $(x, y, z)$ 的方块，如果向下取整、比例和偏移值分别为 $F$、$s$ 和 $o$，第 $n$（$1\le n\le m, n\in N$）个方块函数及其权重分别为 $a_n$ 和 $w_n$，则计算方块坐标的方式为：

> 设 $f(a, b) = \begin{cases}a & a = 0 \\ floor\left(\dfrac{a}{b}\right) & a \ne 0 \end{cases}$，$g(a, b)=\begin{cases}a & a=0 \\ floor\left(\dfrac{a}{b}\right) & a\ne 0\end{cases}$
>
> 取 $v = g\left(f\left(x - o_x, F_x\right), s_x\right) + g\left(f\left(y - o_y, F_y\right), s_y\right) + g\left(f\left(z - o_z, F_z\right), s_z\right)$
>
> 计算各方块函数的权重之和：$w = \displaystyle\sum_{n=1}^{m}{w_n}$
>
> 设 $v'$ = $v \mod w$
>
> 则坐标为 $(x, y, z)$ 的方块使用的方块函数为 $a_i$，其中 $i$ 是满足 $\displaystyle\sum_{n=1}^{i} \ge v'$ 的最小值。
>
> 特别地，当 $w_1 = w_2 = … = w_m$，则坐标为 $(x,y,z)$ 使用的方块函数为 $a_i$，其中 $i = floor(v')$。

## 语法

- `checkerboard(<方块函数 1> [权重 1], <方块函数 2> [权重 2] ... [参数])`

方块函数至少要有 1 个。权重不能为负数，可以为小数或 0，但各项的权重之和不能为 0，每项的权重未指定时，默认为 1。

`参数` 的语法由以下部分任意排列（不可重复）：

- `floor <x> [y] [z]`：增大棋盘格的单元大小。
- `scale <x> [y] [z]`：增加棋盘格的比例，可能形成条纹。
- `offset <x> [y] [z]`：设置棋盘格的偏移值。

上述参数中，如果 `[y]` 未指定，则值为 `<x>`。如果 `[y]` 和 `[z]` 均未指定，则值为 `<x>`。

请注意，方块函数列表之前的各个元素用逗号隔开，方块函数列表与参数之间，以及多个参数之间，用空格隔开。

## 示例

- `checkerboard(white_wool, black_wool)`：由白色羊毛和黑色羊毛组成的棋盘格。对坐标为 `(x,y,z)` 的方块，设 $v=x+y+z$（下同），则当 $v$ 为偶数时白色羊毛，$v$ 为奇数时为黑色羊毛。
- `checkerboard(white_wool, gray_wool, black_wool)`：由白色羊毛、灰色羊毛和黑色羊毛组成的棋盘格。用 3 除 $v$，整除时为白色羊毛，余 1 时为灰色羊毛，余 2 时为黑色羊毛。
- `checkerboard(white_wool)`：仅由白色羊毛组成的棋盘格，所有的方块均为白色羊毛。这样的方块函数有效，但是没有什么意义。
- `checkerboard(white_wool 2, black_wool)`：由白色羊毛和黑色羊毛组成的棋盘格。用 3 除 $v$，整除或余 1 时为白色羊毛，余 2 时为黑色羊毛。
- `checkerboard(white_wool 1.5, black_wool)`：由白色羊毛和黑色羊毛组成的棋盘格。
- `checkerboard(white_wool 0, black_wool 0)`：无效，因为各个方块函数的权重之和为 0。
- `checkerboard(white_wool, black_wool floor 2)`：由白色羊毛和黑色羊毛组成的棋盘格，每格的大小为 2×2×2 的立方体。
- `checkerboard(white_wool, black_wool floor 2 3 4)`：由白色羊毛和黑色羊毛组成的棋盘格，每格的大小为 2×3×4 的立方体。
- `checkerboard(white_wool, black wool scale 2)`：由白色羊毛和黑色羊毛组成的棋盘格，每次计算时各坐标均除以 2。
- `checkerboard(white_wool, black wool scale 1 0 1)`：由白色羊毛和黑色羊毛组成的棋盘格，其中棋盘格忽略 y 坐标。
- `checkerboard(white_wool, black wool offset 1 0 0)`：由白色羊毛和黑色羊毛组成的棋盘格，但是棋盘整体沿 `(1, 0, 0)` 的方向移动。
- `checkerboard(white_wool 1 gray_wool 2, black wool 1 floor 2 scale 2 offset 0 1 0)`：含有所有参数的棋盘格的方块函数。
