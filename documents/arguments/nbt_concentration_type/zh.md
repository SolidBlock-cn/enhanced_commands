# NBT 聚合类型：将多个 NBT 转化为一个

**NBT 聚合类型**（NBT concentration type）是一种[参数类型](../zh.md)，指定了如何将多个 NBT 数据合并为一个。类似于[聚合类型](../concentration_type/zh.md)，但是能处理 NBT 数据。

NBT 聚合类型共支持以下值：

- `first`：第一个值。
- `last`：最后一个值。
- `min`：最小值，仅限当所有 NBT 数据都是数字或都是字符串时才有效。
- `max`：最大值，仅限当所有 NBT 数据都是数字或都是字符串时才有效。
- `list`：将所有 NBT 数据形成一个列表。如果这些数据混合了不同的类型，则会抛出异常。
- `random`：取这些数据的随机值。
- `all`：当这些数据只有一个时，返回这一个数据；如果有多个，则抛出异常，除非一些命令对此有特殊处理，例如 [`/nbt` 命令](../../commands/nbt/zh.md)能够直接显示多个数据。

当聚合的数据为零个值时，除了 `list` 聚合类型之外，都会抛出异常。`list` 聚合类型会将零个数据聚合成空列表。

以下为 NBT 聚合类型在命令中的使用的一些示例：

- `/nbt get entity @a Pos[1] max`：获取所有玩家的纵坐标的最大值。
- `/nbt get entity @e[type=sheep] Color`：获取所有绵羊的羊毛的颜色，这里的聚合类型是采用的默认值 `all`。命令反馈中将显示每个实体的羊毛颜色。
- `/nbt get entity @p[c=5] Health list`：将最近的 5 名玩家的生命值组成一个列表，获取该列表。

以下为 NBT 聚合类型的聚合结果的一些示例：

* 对于 `1b`、`3f`、`2s`：
    * `first`：`1b`
    * `last`：`2s`
    * `min`：`1b`
    * `max`：`3f`
    * `list`：错误
* 对于 `[1]`、`5`、`"x"`：
    * `first`：`[1]`
    * `last`：`"x"`
    * `min`：错误
    * `max`：错误
    * `list`：错误
* 对于 `"iron"`、`"gold"`、`"diamond"`、`"netherite"`：
    * `first`：`iron`
    * `last`：`netherite`
    * `min`：`diamond`
    * `max`：`netherite`
    * `list`：`["iron", "gold", "diamond", "netherite"]`
* 对于没有数据：
    * `first`：错误
    * `last`：错误
    * `min`：错误
    * `max`：错误
    * `list`：`[]`