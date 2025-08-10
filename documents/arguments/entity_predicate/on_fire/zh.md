# `on_fire`：检测实体是否正在着火

此[实体谓词](../zh.md)检测实体是否在着火。参数的值为一个布尔值，支持取反，例如 `[on_fire=!true]` 等价于 `[on_fire=false]`。

## 示例

- `@a[on_fire=false]`：所有没有在着火的玩家。
- `@e[on_fire=true]`：所有正在着火的存活实体。

## 数据结构

- `type`：此处为 `"enhanced_commands:on_fire"`。
- `expected`：布尔值。