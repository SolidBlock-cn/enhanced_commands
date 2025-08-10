# `sneaking`：检测潜行

此[实体谓词](../zh.md)测试实体是否正在潜行。飞行模式下玩家下降时也会被选中。此选项与战利品表中的谓词不同。

## 示例

- `@e[sneaking=true]`：选择所有正在潜行的存活实体。
- `@e[sneaking=false]`：选择所有未在潜行的存活实体。

此参数可以取反，但是通常没有必要。例如，`@e[sneaking=!true]` 等价于 `@e[sneaking=false]`。

## 数据结构

- `type`：此处为 `"enhanced_commands:sneaking"`。
- `expected`：布尔值。