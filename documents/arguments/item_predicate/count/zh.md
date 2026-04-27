# `count()` 物品谓词：测试物品的数量

此[物品谓词](../zh.md)用于测试物品堆叠的物品的个数。可以匹配一个值，或者数值范围。

## 语法

`count(<范围>)`

## 示例

- `iron_ingot[count(3)]`
- `diamond[count(..12)]`
- `#planks[count(10..20)]`

## 数据结构

- `type`：此时为 `"enhanced_commands:count"`。
- `count`：整数或复合标签，若为复合标签，有以下字段：
    - `min`：整数。
    - `max`：整数。