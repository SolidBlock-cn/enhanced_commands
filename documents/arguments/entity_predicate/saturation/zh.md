# `saturation`：检测实体的饱和度

此[实体谓词](../zh.md)测试实体的饱和度。可以接受浮点数范围。可以取反。和 [`food`](../food/zh.md) 类似，加入此参数将只能选择玩家。

## 示例

- `@e[saturation=5..]`：选择饱和度至少为 5 的玩家。
- `@e[saturation=!0]`：选择饱和度不为 0 的玩家。

## 数据结构

- `type`：此处为 `"enhanced_commands:saturation"`。
- `saturation`：浮点数范围。
- `inverted`：布尔值，可选，默认为 `false`。