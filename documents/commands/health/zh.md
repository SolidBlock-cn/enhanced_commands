# `/health`

此[命令](../zh.md)用于修改生物的生命值。此命令对于非生物实体，包括矿车、掉落的物品等，不起作用。

将生命值设置为 0 时，会直接导致该生物死亡，并且没有死亡消息。

## 语法

- `/health get [实体] [聚合类型] [倍率]`：获取生物的生命值。
- `/health set <实体> <值>`：设置生物的生命值。
- `/health add [实体] [值]`：增加生物的生命值。
- `/health remove [实体] [值]`：减少生物的生命值。

## 参数

### `<实体>`

[实体选择器](/documents/arguments/entity_selector/zh.md)，该命令所需要影响到的实体。对于除 `/health set` 之外的命令，默认为命令执行者，即 `@s`。

### `[聚合类型]`

默认为 `average`。当选择到多个生物时，指定如何返回值。

### `[倍率]`

浮点数。默认为 1。不影响命令反馈，影响命令的返回值（即 `/execute store result ...` 接收到的值）。

### `[值]`

浮点数。对于 `/health add`，当未指定时，会将生物的生命值加到最大值。对于 `/health remove`，当未指定时，会将生物的生命值减少到零。

## 示例

- `/health get`：获取自己的生命值。
- `/health set @a 5`：将所有玩家的生命值设置为 5。
- `/health add @e`：将所有生物的生命值加满。
- `/health remove @a[distance=..5] 1`：扣除距离 5 格内的生物 1 生命值。

## 参见

- [实体选择器中的 health 参数](/documents/arguments/entity_selector/zh.md#health)