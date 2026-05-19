# `/velocity`：修改实体的速度

此[命令]用于修改实体的速度。

## 语法

- `/velocity get [目标] [聚合类型]`
- `/velocity set <目标> <向量>`：设置实体的速度。
- `/velocity add <目标> <向量>`：将实体的速度增加一个向量。
- `/velocity subtract <目标> <向量>`：将实体的速度减去一个向量。
- `/velocity scale <目标> <倍率>`：将实体的速度乘以指定的倍率（数量）。
- `/velocity multiply <目标> <向量>`：将实体的速度向量的各分量与指定的向量的各分量分别相乘。例如，如果实体原速度为 $\vec a = (a_x, a_y, a_z)$，指定的向量为 $\vec b = (b_x, b_y, b_z)$，则实体的速度将被修改为 $(a_x b_x, a_y b_y, a_z b_z)$。
- `/velocity cross <目标> <向量>`：计算指定当前速度向量与指定向量的向量积，作为实体的新速度。例如，如果实体原速度为 $\vec a$，指定的向量为 $\vec b$，则实体的速度将被修改为 $\vec a \times \vec b$。
- `/velocity setsize <目标> <大小>`：修改实体的速度的大小，且如果大小为正，则方向不变，如果大小为负，则方向相反。若原来的实体的速度为 0，则不执行操作。例如，如果实体的原速度为 $\vec a$（$\vec a \ne \vec 0$），指定的大小为 $s$，则实体的速度将被修改为 $\dfrac {\vec a}{\left |\vec a\right |} \cdot s$。

## 参数

### `<目标>`

[实体选择器](/documents/arguments/entity_selector/zh.md)。将被修改速度的实体。

### `<向量>`

由三个浮点数组成的向量。

> 目标支持使用相对坐标和局部坐标，但不建议，因为将相对坐标或局部坐标用作实体的速度是没有意义的。

### `<倍率>`/`<大小>`

双精度浮点数。

### '[聚合类型]'

[聚合类型](/documents/arguments/concentration_type/zh.md)，若未指定，则为 `average`。

## 示例

- `/velocity get @s`
- `/velocity get @a average`
- `/velocity get @e max`
- `/velocity set @s 0 1 0`
- `/velocity add @e 0 0.5 0`
- `/velocity subtract @e 0 -0.5 0`：与上一个例子等价。
- `/velocity scale @a 1.6`
- `/velocity multiply @a 1.6 1.6 1.6`：与上一个例子等价。
- `/velocity multiply @a 1 0 1`
- `/velocity cross @e[type=pig] 0 1 0`
- `/velocity setsize @a 0.5`