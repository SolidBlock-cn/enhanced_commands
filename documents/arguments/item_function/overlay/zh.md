# `overlay()`

此[物品函数](../zh.md)用于依次应用多个物品函数。

## 语法

`overlay([物品函数], ...)`

可以没有物品函数，表示不执行任何操作，直接返回参数。

## 示例

- `overlay(netherite_sword, enchant(any_supported, random_reasonable))`：给予一个附了任意一个受支持的魔咒、有随机的合理等级的下界合金剑，等价于 `netherite_sword*enchant(any_supported, random_reasonable)`，等效于 `netherite_sword[enchant(any_supported, random_reasonable)]`。

## 数据结构

- `type`：此时为 `enhanced_commands:overlay`。
- `functions`：列表。
    - 物品函数。

## 参见

- [`overlay()` 方块函数](../../block_function/overlay/zh.md)
- [`overlay()` NBT 函数](../../nbt_function/overlay/zh.md)