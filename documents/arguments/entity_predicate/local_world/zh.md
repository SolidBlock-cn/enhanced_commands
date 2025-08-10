# `local_world`：筛选当前世界玩家的特殊谓词

此[实体谓词](../zh.md)仅当实体与命令执行者处于同一世界时通过。这是特殊谓词，无法通过实体选择器指定。指定了 `distance`、`dx` 等参数的实体选择器会带有此谓词。

## 数据结构

- `type`：此时为 `"enhanced_commands:local_world`。