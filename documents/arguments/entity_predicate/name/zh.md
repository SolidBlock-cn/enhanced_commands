# `name`：测试实体的名字。

此[实体谓词](../zh.md)和原版的实体选择器的 `name` 参数一致，测试实体的名字，支持取反。

## 示例

- `@a[name=Steve]`
- `@e[name=!Alex]`

## 数据结构

- `type`：此处为 `"enhanced_commands:name"`。
- `name`：字符串。
- `inverted`：布尔值，可选，默认为 `false`。