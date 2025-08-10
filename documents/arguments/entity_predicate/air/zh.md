## `air`：测试实体的空气值

此[实体谓词](../zh.md)测试实体的空气值。可以接受整数范围或关键字 `max` 表示最大空气值。可以取反。

## 示例

- `@e[air=5..]`：选择空气值不少于 5 的实体。
- `@a[air=!..0]`：选择空气值大于 0 的实体。

## 参见

- [`/air` 命令](/documents/commands/air/zh.md)。

## 数据结构

当类型为 `air` 时：

* `type`：此时为 `"enhanced_commands:air"`。
* `air`：整数范围。
* `inverted`：布尔值，可选，默认为 `false`。

当类型为 `air_max` 时：

* `type`：此时为 `"enhanced_commands:air_max"`。
* `inverted`：布尔值，可选，默认为 `false`。
