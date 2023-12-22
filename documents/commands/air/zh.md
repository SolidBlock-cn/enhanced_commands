# `/air`

此[命令](../zh.md)用于修改实体的空气值。此命令对于任何实体都有效，但一些非生物实体的空气值可能没有实际的影响。

## 语法

- `/air get [实体] [聚合类型]`：获取生物的空气值。
- `/air get [实体] [聚合类型] store <NBT 目标> <路径>`：获取生物的空气值，并以 NBT 的形式存在该整数值。
- `/air set <实体> <值>`：设置生物的空气值。
- `/air set <实体> from (result|success) ...`：执行一个命令，并将命令执行的整数结果设置为生物的空气值。
- `/air set <实体> from of <实体> [聚合类型]`：将生物的空气值设置为其他实体的空气值相等的数值。
- `/air set <实体> from <NBT 源> <路径>`：获取指定来源的 NBT 的整数值，并实体的空气值设置为此浮点数值。
- `/air add [实体] [值]`：增加生物的空气值。
- `/air remove [实体] [值]`：减少生物的空气值。

## 参数

### `<实体>`

[实体选择器](/documents/arguments/entity_selector/zh.md)，该命令所需要影响到的实体。对于除 `/air set` 之外的命令，默认为命令执行者，即 `@s`。

### `[聚合类型]`

[聚合类型](/documents/arguments/concentration_type/zh.md)，默认为 `average`。当选择到多个实体时，指定如何返回值。

### `[值]`

整数。对于 `/air add`，当未指定时，会将实体的空气值加到最大值（美西螈 6000，海豚 4800，其他生物 300）。对于 `/air remove`，当未指定时，会将实体的空气值减少到零。

### `<NBT 目标> <路径>`

将生命值以浮点数的形式存储至的位置。参见 [NBT 目标](/documents/arguments/nbt_target/zh.md)。

### `<NBT 源> <路径>`

指定需要获取浮点形式的 NBT 值的位置。参见 [NBT 源](/documents/arguments/nbt_source/zh.md)。

## 示例

- `/air get`：获取自己的空气值。
- `/air set @a 3`：将所有玩家的空气值设置为 3。
- `/air add @e`：将所有实体的空气值加满。
- `/air remove @a[distance=..5] 10`：扣除距离 5 格内的实体 10 空气值。

## 参见

- [实体选择器中的 air 参数](/documents/arguments/entity_selector/zh.md#air)