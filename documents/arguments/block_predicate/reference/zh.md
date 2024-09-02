# 引用方块谓词

此[方块谓词](../zh.md)引用数据包中的方块谓词 ID。

ID 为 `<namespace>:<path>` 的方块谓词位于数据包中的 `data/<namespace>/enhanced_commands/block_predicate/<path>.json`。

## 语法

- `$<ID>`

参数 `<ID>` 为数据包中的方块谓词的 ID。未指定命名空间时，命名空间为 `enhanced_commands`。

## 示例

- `$air_grid`（等价于 `$enhanced_commands:air_grid`）

## 参见

- [`引用方块函数`](../../block_function/reference/zh.md)