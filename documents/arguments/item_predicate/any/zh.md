# `any()`：满足任何一个即可

多个[物品谓词](../zh.md)的并集。只要有其中一个物品谓词通过，那么这个物品谓词就会通过。

## 语法

`any([物品谓词], ...)`

如果物品谓词多于 0 个，则等同于 `<物品谓词 1>}<物品谓词 2>` 的语法，参见[复合物品谓词](../zh.md#复合物品谓词)。

## 示例

- `any(dirt, grass_block)`：当物品是泥土或者草方块时通过。
- `any(dirt, grass_block, mycelium)`：当物品是泥土、草方块或者菌丝体时通过。
- `any(dirt)`：当方块是泥土时通过。等效于 `dirt`。
- `any()`：始终不通过。等效于 `!*`。

## 数据结构

- `type`：此时为 `"enhanced_commands:any"`。
- `predicates`：列表。
    - 一个物品谓词。

## 参见

- [`all()`](../all/zh.md)
- [`any()` 方块谓词](../../block_predicate/any/zh.md)
- [`any()` NBT 谓词](../../nbt_predicate/nbt/zh.md)