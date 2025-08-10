# `advancements`：测试玩家的进度

此[实体谓词](../zh.md)测试玩家的进度。其原理和原版相同，区别在于在指定进度准则时，可以使用带引号的字符串。

## 语法

- `[advancements={<进度 id> = <布尔值>, ...}]`
- `[advancements={<进度 id> = {<准则> = <布尔值>, ...}, ...}]`

## 数据结构

- `type`：此时为 `"enhanced_commands:advancements`。
- `advancements`：映射。
    - `<进度 id>`：布尔值或映射，若为映射，有以下字段：
        - `<准则 id>`：布尔值。