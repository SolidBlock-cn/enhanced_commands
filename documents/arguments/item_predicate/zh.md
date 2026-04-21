# 物品谓词

**物品谓词**（item predicate）是一种[参数类型](../zh.md)，可用于测试一个物品堆叠（item stack）是否满足条件。一个物品堆叠可能匹配此谓词（测试通过），也可能不匹配此谓词（测试失败）。

## 基本用法

物品谓词可以像原版一样，使用物品 ID和物品标签来匹配物品，可以省略命名空间。此外，还可以指定物品谓词。例如：

- `diamond`：匹配任何的钻石。等价于 `minecraft:diamond`，下面的例子均省略了物品 ID 的命名空间。
- `#stairs`：匹配任何的楼梯。
- `iron_sword[enchantments]`：匹配任何拥有 `minecraft:enchantments` 组件的铁剑。等价于 `minecraft:iron_sword[minecraft:enchantments]`，下面的例子均省略组件类型 ID 的命名空间。
- `#axes[damage=2]`：匹配任何损伤值为 2 的斧。

## 复合物品谓词

多个物品谓词可以进行复合，符号 `|` 取交集，符号 `&` 取并集。`!` 可用于将一个谓词取反。具体用法类似于[方块谓词#复合方块谓词](../block_predicate/zh.md#复合方块谓词)，本页不再赘述。

例如：

- `grass_block|dirt`：草方块或泥土。
- `#swords|#axes|mace`：任意的剑、任意的斧或重锤。
- `#piglin_loved&#swords`：猪灵喜欢的剑。
- `!#piglin_loved&#swords`：不是猪灵喜欢的物品，但仍要求是剑。
- `!(#piglin_loved&#swords)`：不是猪灵喜欢的剑，可以是任意不是猪灵喜欢的物品，也可以是任意不是剑的物品。
- `#piglin_loved&(#swords|#axes)`：猪灵喜欢的剑或斧。
- `#piglin_loved&#swords|#axes`：猪灵喜欢的剑，或者是斧（无论猪灵是否喜欢）。

## 物品谓词的函数语法

物品谓词支持使用函数语法。目前支持的函数包括：

- `all()`
- `any()`
- `count()`
- `probability()`

[//]: # (todo 完善物品谓词的函数语法)

## 物品组件列表中的括号和函数语法

在物品组件列表中，可以使用括号并在里面表示任意的谓词，也可以直接表示一个函数，相当于叠加一个同是需要满足的条件。

- `#wool[count(3..5)]`：数量在 3 到 5 之间的羊毛。在效果上等价于 `#wool&count(3..5)`。
- `#planks[(#non_flammable_wood)]`：既是木板，又是不可在熔炉内燃烧的物品。在效果上等价于 `#wool&#non_flammable_wood`。
- `#planks[#non_flammable_wood]`：无效，因为在物品组件列表中使用非函数语法表示其他物品组件时，必须加括号。

上述语法可以像常规的物品组件的键值对一样，与其他的各个项并列。例如：

- `*[map_id, max_damage=2, (!#axes), count(5..)]`