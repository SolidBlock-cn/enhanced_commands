# `loot_table_predicate`：根据战利品表谓词测试实体

此[实体谓词](../zh.md)会利用战利品表谓词测试实体。与原版的 `predicate` 参数不同的是，本模组的 `predicate` 参数支持直接指定战利品表谓词 JSON。

## 数据结构

- `type`：此处为 `"enhanced_commands:loot_table_predicate"`。
- `predicate`：字符串或映射。
- `inverted`：布尔值，可选，默认为 `false`。