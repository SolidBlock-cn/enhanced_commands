# `/food`

此[命令](../zh.md)用于修改玩家的饱和度、饱食度或消耗度。仅对玩家有效。

## 语法

- `/food get [玩家] [聚合类型]`：获取玩家的饱和度、饱食度和消耗度。
- `/food setfood <玩家> <饱食度> [饱和度]`：设置玩家的饱食度，可一并设置饱和度。
- `/food setsaturation <玩家> <饱和度>`：设置玩家的饱和度。
- `/food setexhaustion <玩家> <消耗度>`：设置玩家的消耗度。
- `/food add [玩家]`：将玩家的饱食度和饱和度增加到最大值，并移除所有的消耗度。
- `/food add [玩家] [饱食度] [饱和度倍率]`：增加玩家的饱食度和饱和度，实际运行时会受到值的数量上的限制（饱食度不能超过 20，饱和度不能超过饱食度）。
- `/food add <玩家> item [物品]`：增加玩家的饱食度和饱和度，增加的量为该物品所产生的饱食度和饱和度（不会带来该物品的副作用，如效果等，下同）。默认为玩家手上的物品。
- `/food add <玩家> from [槽位]`：增加玩家的饱食度和饱和度，增加的量为该玩家指定槽位的物品所产生的饱食度和饱和度。默认为玩家手上的物品。

## 参数

### `<玩家>`

[实体选择器](/documents/arguments/entity_selector/zh.md)，默认为命令的执行者，即 `@s`。必须是玩家。

### `[聚合类型]`

默认为 `average`。指定当选择到多个值时，应该返回哪个值。

### `<饱食度>`

整数。尽管没有数值上的限制，但是饱食度一般在 0 到 20 的范围内。

### `<饱和度>`

浮点数。尽管没有数值上的限制，但是饱和度一般大于 0 且不能超过玩家的饱食度。

### `<消耗度>`

浮点数。尽管没有数值上的限制，但是消耗度一般不超过 40。

## 示例

- `/food get Steve`：获取 Steve 的饱食度、饱和度和消耗度。
- `/food get @a max`：获取所有玩家的饱食度、饱和度和消耗度的最大值。
- `/food setfood @a 15`：将所有玩家的饱食度设置为 15。
- `/food setfood @a 20 0`：将所有玩家的饱食度设置为 20，饱和度设置为 0。
- `/food setsaturation @r 20`：将随机一名玩家的饱和度设置为 20（忽略饱和度不能超过饱食度的限制）。
- `/food add @s`：将自己的饱食度和饱和度加满，并清除所有的消耗度。
- `/food add @s 5`：将自己的饱食度增加 5，但不超过 20。
- `/food add @s 5 0.5`：将自己的饱食度增加 5，但不超过 20，饱和度增加 2.5，但不超过饱食度。
- `/food add @s from`：增加自己的饱食度和饱和度，增加的量为自己手中持有的物品所提供的饱食度和饱和度。例如，当手中持有胡萝卜时，会增加胡萝卜所提供的饱食度和饱和度（不会使手中的萝卜减少）。当手中持有的物品不是食物时没有影响。注意：蛋糕不能直接拿在手中吃，所以也不被视为食物。
- `/food add @a item cooked_beef`：增加所有玩家的饱食度和饱和度，增加的量为熟牛排所提供的饱食度和饱和度。
- `/food add @a item cake`：没有效果，因为蛋糕不能直接拿在手中食用，不带来效果。
- `/food add @s item enchanted_golden_apple`：增加自己的饱食度和饱和度，增加的量为附魔金苹果所提供的饱食度和饱和度，但是不会产生附魔金苹果所带来的伤害吸收。抗性提升等效果。
- `/food add @s item`：等价于 `/food add @s from`。

## 参见

[实体选择器](/documents/arguments/entity_selector/zh.md)中的 [`food`](/documents/arguments/entity_selector/zh.md#food)、[`saturation`](/documents/arguments/entity_selector/zh.md#saturation) 和 [`exhaustion`](/documents/arguments/entity_selector/zh.md#exhaustion) 参数。