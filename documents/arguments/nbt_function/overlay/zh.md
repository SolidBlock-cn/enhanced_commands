# `overlay()`

此 [NBT 函数](../zh.md)用于依次应用多个 NBT 函数。

## 语法

`overlay([NBT 函数], ...)`

可以没有 NBT 函数，表示不执行任何操作，直接返回参数，但此时如果参数为空，会抛出错误。

## 示例

- `overlay(string_replace(red, green), string_replace(concrete, terracotta))`：先将字符串中的 `"red"` 替换为 `"green"`，再将字符串中的 `"concrete"` 替换为 `"terracotta"`。等价于 `string_replace(red, green)|string_replace(concrete, terracotta)`。

## 数据结构

- `type`：此时为 `enhanced_commands:overlay`。
- `functions`：列表。
    - NBT 函数。

## 参见

- [`overlay()` 方块函数](../../block_function/overlay/zh.md)
- [`overlay()` 物品函数](../../item_function/overlay/zh.md)