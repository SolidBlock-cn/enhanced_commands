# `health`：测试实体生命值

此[实体谓词](../zh.md)测试实体的生命值，可以接受一个范围或者关键字 `max`。参数可以取反。无论参数是否取反，此参数都将只能够选择生物的生命值，矿车、掉落的物品等实体虽有特殊的生命值但是不会实体选择器用于选择。

## 示例

- `@e[health=20]`：选择所有生命值为 20 的生物。
- `@e[health=!20]`：选择所有生命值不为 20 的生物。
- `@e[health=max]`：选择生命值为最大值的生物。
- `@e[health=10..]`：选择生命值不小于 10 的生物。

## 参见

- [`/health` 命令](/documents/commands/health/zh.md)

## 数据结构

如指定生命值为取值范围：

- `type`：此处为 `"enhanced_commands:health"`。
- `health`：浮点数取值范围。
- `inverted`：布尔值，可选，默认为 `false`。

如指定生命值为最大值：

- `type`：此处为 `"enhanced_commands:health_max"`。
- `inverted`：布尔值，可选，默认为 `false`。