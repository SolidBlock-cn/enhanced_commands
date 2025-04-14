# `checkerboard()`：棋盘格的谓词

此[方块谓词](../zh.md)生成一个棋盘格，根据棋盘格为每个位置确定谓词。参见 [`checkerboard() 方块函数`](../../block_function/checkerboard/zh.md)。

## 语法

- `checkerboard(<方块谓词 1> [权重 1], <方块谓词 2> [权重 2] ... [参数])`

方块谓词至少要有 1 个。权重不能为负数，可以为小数或 0，但各项的权重之和不能为 0，每项的权重未指定时，默认为 1。

`参数` 的语法参见 [`checkerboard()` 方块函数](../../block_function/checkerboard/zh.md)。

## 示例

- `checkerboard(*, !*)`：对坐标为 `(x,y,z)` 的方块，设 $v=x+y+z$（下同），则当 $v$ 为偶数时始终通过（采用方块谓词 `*`），$v$ 为奇数时为始终不通过（采用方块谓词 `!*`）。
- `checkerboard(*, !*, rand(0.5))`：用 3 除 $v$，整除时始终不通过，余 1 时不通过，余 2 时有 0.5 的概率通过。
- `checkerboard(oak_planks)`：当方块是橡木木板时通过。这样的方块谓词有效，但是没有什么意义。
- `checkerboard(* 2, !*)`：用 3 除 $v$，整除或余 1 时为通过，余 2 时为不通过。

## 参见

- [`checkerboard()` 方块函数](../../block_function/checkerboard/zh.md)