# 物品谓词

**物品谓词**（item predicate）是一种[参数类型](../zh.md)，可用于测试一个物品堆叠（item stack）是否满足条件。一个物品堆叠可能匹配此谓词（测试通过），也可能不匹配此谓词（测试失败）。

> 本模组直接修改了原版的物品谓词的写法，并在原版添加的各个谓词中注入信息以使之能够被序列化。理论上，其在 `/clear` 等命令中的行为与原版一致，但可能存在细微差异。其他模组也可能扩充原版的物品谓词的功能，但这些不一定能被此模组识别，这种情况下的谓词通常能正常生效，但不一定能被正常序列化。

## 基本用法

物品谓词可以像原版一样，使用物品 ID 和物品标签来匹配物品，可以省略命名空间。此外，还可以指定物品组件。例如：

- `diamond`：匹配任何的钻石。等价于 `minecraft:diamond`，下面的例子均省略了物品 ID 的命名空间。
- `#stairs`：匹配任何的楼梯。
- `iron_sword[enchantments]`：匹配任何拥有 `minecraft:enchantments` 组件的铁剑。等价于 `minecraft:iron_sword[minecraft:enchantments]`，下面的例子均省略组件类型 ID 的命名空间。
- `#axes[damage=2]`：匹配任何损伤值为 2 的斧。
- `*`：匹配任意的物品。

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

- [`all()`](all/zh.md)
- [`any()`](any/zh.md)
- [`count()`](count/zh.md)
- [`probability()`](probability/zh.md)

## 物品组件列表

像原版一样，物品组件列表可以直接添加到物品谓词后（不一定是物品 ID、物品标签或星号）。

在物品组件列表中，可以使用括号并在里面表示任意的物品谓词，也可以直接使用函数式语法，相当于叠加一个同是需要满足的条件。

- `#wool[count(3..5)]`：数量在 3 到 5 之间的羊毛。在效果上等价于 `#wool&count(3..5)`。
- `#planks[(!pale_oak_planks)]`：既是木板，又不是苍白橡木木板的物品。在效果上等价于 `#wool&!pale_oak_planks`。
- `#planks[(#non_flammable_wood)]`：既是木板，又是不可在熔炉内燃烧的物品。在效果上等价于 `#wool&#non_flammable_wood`。
- `#planks[#non_flammable_wood]`：无效，因为在物品组件列表中使用非函数语法表示其他物品组件时，必须加括号。

上述语法可以像常规的物品组件的键值对一样，与其他的各个项并列。例如：

- `*[map_id, max_damage=2, (!#axes), count(5..)]`

> 根据原版（1.20.5 及以上）的物品谓词的解析方式，中括号前面可以有空格，例如 `#axes[damage=2]` 可以写成 `#axes [damage=2]`，但这种情况可能导致命令的解析不稳定，且本模组中的其他相似语法（如方块谓词、方块函数、物品函数）均不允许在中括号前添加空格，因此也不建议在使用物品谓词时在中括号前添加空格。
>
> 物品组件列表是先于复合物品组件的语法解析的。例如，`stone|blackstone[count=1]` 相当于 `stone|(blackstone[count=1])`。如果要让物品组件列表中的函数应用于整个复合物品组件，需要添加括号，如 `(stone|blackstone0[count=1])`。

## 数据结构

- `type`：ID，默认命名空间为 `enhanced_commands`。以下省略了命名空间。
- （当 `type` 为 `component_presence`：）
- `component_type`：ID，物品组件类型的 ID。
- （当 `type` 为 `component_value_check`：）
- `component_type`：ID，物品组件类型的 ID。
- `value`：物品组件的预期值，数据类型取决于物品组件类型。
- （当 `type` 为 `constant`：）
- `value`：布尔值。
- （当 `type` 为 `negating`：）
- `predicate`：物品组件。
- （当 `type` 为 `simple`：）
- `item`：ID。
- （当 `type` 为 `simple_combination`：）
- `item_type`：物品谓词。
- `components`：列表。
    - 物品谓词。当此谓词为类型为 `component_presence` 或 `component_value_check` 时，通常表示直接在组件列表中指定组件，如果是其他类型，则表示在组件列表中使用函数语法，或者使用被括号括起来的另一个物品谓词。
- （当 `type` 为 `tag`：）
- `items`：物品 ID、物品标签，或由物品 ID 组成的列表。
- （当 `type` 为 `unknown`：）
- 没有额外字段。此类型通常表示在原版的物品谓词解析过程中未被本模组识别的谓词。
- （其他类型的物品谓词的数据结构，请参见各个子页面。）