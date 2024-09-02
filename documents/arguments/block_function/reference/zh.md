# 引用方块函数

此[方块函数](../zh.md)引用数据包中的方块函数 ID。

ID 为 `<namespace>:<path>` 的方块函数位于数据包中的 `data/<namespace>/enhanced_commands/block_function/<path>.json`。

## 语法

- `$<ID>`

参数 `<ID>` 为数据包中的方块函数的 ID。未指定命名空间时，命名空间为 `enhanced_commands`。

## 示例

- `$white_checkerboard`（等价于 `$enhanced_commands:white_checkerboard`）

## 参见

- [`引用方块谓词`](../../block_predicate/reference/zh.md)