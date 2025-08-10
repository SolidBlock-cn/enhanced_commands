# `exhaustion`：测试实体的消耗度

此[实体谓词](../zh.md)测试实体的消耗度。可以接受浮点数范围。可以取反。和 [`food`](../food/zh.md) 类似，加入此参数将只能选择玩家。

## 示例

- `@a[exhaustion=0.5..]`：选择消耗度至少为 0.5 的玩家。

## 参见

- [`/food` 命令](/documents/commands/food/zh.md)。

## 数据结构

- `type`：此时为 `"enhanced_commands:exhaustion"`。
- `exhaustion`：浮点数取值范围。
- `inverted`：布尔值，可选，默认为 `false`。