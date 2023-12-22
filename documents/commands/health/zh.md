# `/health`

此[命令](../zh.md)用于修改生物的生命值。此命令对于非生物实体，包括矿车、掉落的物品等，不起作用。

将生命值设置为 0 时，会直接导致该生物死亡，并且没有死亡消息。

## 语法

- `/health get [实体] [聚合类型] [倍率]`：获取生物的生命值。
- `/health get [实体] [聚合类型] store <NBT 目标> <路径>`：获取生物的生命值，并以 NBT 的形式存在该浮点数值。
- `/health set <实体> <值>`：设置生物的生命值。
- `/health set <实体> from (result|success) ...`：执行一个命令，并将命令执行的整数结果设置为生物的生命值。
- `/health set <实体> from of <实体> [聚合类型]`：将实体的生命值设置为其他实体的生命值相等的数值。
- `/health set <实体> from <NBT 源> <路径>`：获取指定来源的 NBT 的浮点数值，并实体的生命值设置为此浮点数值。
- `/health add [实体] [值]`：增加生物的生命值。
- `/health remove [实体] [值]`：减少生物的生命值。

## 参数

### `<实体>`

[实体选择器](/documents/arguments/entity_selector/zh.md)，该命令所需要影响到的实体。对于除 `/health set` 之外的命令，默认为命令执行者，即 `@s`。

### `[聚合类型]`

[聚合类型](/documents/arguments/concentration_type/zh.md)，默认为 `average`。当选择到多个生物时，指定如何返回值。

### `[倍率]`

浮点数。默认为 1。不影响命令反馈，影响命令的返回值（即 `/execute store result ...` 接收到的值）。

### `[值]`

浮点数。对于 `/health add`，当未指定时，会将生物的生命值加到最大值。对于 `/health remove`，当未指定时，会将生物的生命值减少到零。

### `<NBT 目标> <路径>`

将生命值以浮点数的形式存储至的位置。参见 [NBT 目标](/documents/arguments/nbt_target/zh.md)。

### `<NBT 源> <路径>`

指定需要获取浮点形式的 NBT 值的位置。参见 [NBT 源](/documents/arguments/nbt_source/zh.md)。

## 示例

- `/health get`：获取自己的生命值。
- `/health set @a 5`：将所有玩家的生命值设置为 5。
- `/health set Player1 from success kill Player2`：杀死 Player2，如果成功杀死则将 Player 1 的生命值设置为 1，否则设置为 0。
- `/health set Player1 from scoreboard players get Player2 example_objective`：将 Player1 的生命值设置为 Player2 在 example_objective 计分项上的分数的值。
- `/health set @s from of @p[type=!player]`：将自己的生命值设置为距离最近的非玩家实体的生命值相等的值。
- `/health set @s from entity @s Pos[1]`：将自己的生命值设置为自己的 Y 坐标的值。
- `/health add @e`：将所有生物的生命值加满。
- `/health remove @a[distance=..5] 1`：扣除距离 5 格内的生物 1 生命值。

## 参见

- [实体选择器中的 health 参数](/documents/arguments/entity_selector/zh.md#health)