## `baby`：选择幼年实体

此[实体谓词](../zh.md)测试实体是否为幼年实体，接收一个布尔值。如果实体类型不存在幼年实体，则视为未幼年实体。

## 示例

- `@e[baby=true]`：选择所有幼年实体。
- `@e[baby=false]`：选择所有非幼年（包括不存在幼年状态）的实体。

此参数可以取反。例如，`@e[baby=!true]` 等价于 `@e[baby=false]`。

## 数据结构

- `type`：此时为 `"enhanced_commands:baby"`。
- `expected`：布尔值。