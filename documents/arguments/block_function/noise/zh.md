# `noise()`：随心所欲生成方块噪声

此[方块函数](../zh.md)用于生成噪声。噪声由多个方块函数组成，每个方块函数都可以有各自的权重。噪声也可以有不同的选项。目前仅采用双柏林噪声采样，噪声算法采用于原版 Minecraft 一致的算法。

## 语法

- `noise(<方块函数 1> [权重 1], <方块函数 2> [权重 2], ...)`
- `noise(<方块函数 1> [权重 1], <方块函数 2> [权重 2], ...; seed = <种子>, first_octave = <主倍频>, amplitudes = <振幅>, scale = <x> [y] [z]; offset = <x> [y] [z])`

分号后的命名参数都是可选的，并且可以调换顺序，但是每个参数不能重复出现。

## 参数

### 方块函数和权重列表

指定该噪声可以应用的方块函数以及每个方块函数的权重。未指定权重则权重默认为 1。权重不能为负数，可以为 0，但是所有方块函数的权重之和不能为 0。

### seed

用于生成该噪声的种子。同一种子在相同位置生成相同的噪声。

### first_octave

主倍频。整数，默认为 -1。

### amplitudes

振幅。双精度浮点数，可以支持多个数，多个数用空格隔开。

### scale

坐标的缩放。可以是 1~3 个值，格式为 `<x> [y] [z]`，用空格隔开。

- 当 `[z]` 未指定时，与 `<x>` 相同。
- 当 `[y] [z]` 均为指定时，均与 `<x>` 相同。

### offset

噪声生成效果的偏移。可以是 1~3 个值，格式为 `<x> [y] [z]`，用空格隔开。效果与 [`checkerboard()` 的 offset 参数](../checkerboard/zh.md) 类似。

## 示例

- `noise(white_concrete, black_concrete)`：由白色混凝土和黑色混凝土组成的噪声。
- `noise(white_concrete, black_concrete; first_octave = -2)`：由白色混凝土和黑色混凝土组成的噪声，主倍频为 -2。
- `noise(white_concrete, black_concrete; scale = 0.5 0)`：由白色混凝土和黑色混凝土组成的噪声，在 x 和 z 方向上拉伸一倍，在 y 方向上不应用噪声。
- `noise(white_concrete, black_concrete; first_octave = -3, amplitudes = 1 2 3)`：由白色混凝土和黑色混凝土组成的噪声，主倍频为 -3，并指定振幅为 1 2 3。
- `noise(white_concrete, black_concrete 0.5)`：由白色混凝土和黑色混凝土组成的噪声，其中黑色混凝土的权重为 0.5（因此白色混凝土和黑色混凝土的权重比为 2:1）。
- `noise(white_concrete 2, black_concrete)`：和上一个例子等价。