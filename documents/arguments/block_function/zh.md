# 方块函数

**方块函数**（block function）是一种[参数类型](../zh.md)，用于对世界内的特定位置的方块（包括方块实体）进行修改。

## 基本用法

方块函数可以像原版一样，使用方块 ID 并且还可以指定方块状态属性和 NBT。通常来说，这些情况下会直接忽略原先的方块。例如：

- `minecraft:stone`：石头。
- `minecraft:redstone_lamp[lit=true]`：亮着的红石灯。
- `minecraft:repeating_command_block{Auto: true}`：自动执行的循环型命令方块。

## 复合方块函数

多个方块函数可以进行复合，从而产生更加复杂的方块函数。

- `<方块函数 1>*<方块函数 2>`：两个（或者多个）方块函数依次应用。第一个函数应用的结果会作为第二个函数的参数。
- `<方块函数 1>|<方块函数 2>`：两个（或者多个）方块函数随机选择。会随机从这两个中选择一个方块函数来应用。

上面这两个方块函数的语法顺序为先 `*` 再 `|`。例如：`a|b*c` 相当于 `a|(b*c)`。

上面的这些语法不能有空格，但是如果位于括号内，则可以有空格。例如 `a | b` 是无效的，但是 `(a | b)` 和 `dry(a | b)` 是有效的。

例如（下面的这些例子中的 ID 均省略了命名空间）：

- `stone|dirt|grass_block`：随机选择石头、泥土和草方块。
- `black_wool|white|wool`：随机选择黑色羊毛和白色羊毛。

## 完整列表

### 非函数语法

- [简单方块函数：`方块id`](simple/zh.md)：简单地使用指定方块 ID 的方块。
- [标签方块函数：`#标签id`](tag/zh.md)：从方块标签中随机选择一个方块来应用。
- [属性方块函数：`[属性=值]`](property_names/zh.md)：对当前方块的属性进行修改。
- [NBT 方块函数：`{}`](nbt/zh.md)：修改方块的 NBT 数据。
- [属性 NBT 组合：`方块id[...]{...}`](property_nbt_combination/zh.md)：同时设置方块的 ID 或标签、方块状态属性以及 NBT，这类似于原版的用法。
- [随机方块函数：`*`](random/zh.md)：随机选择一个方块并随机选择一个方块状态。
- [原始方块函数：`~`](use_original/zh.md)：使用整个函数计算过程之前的方块。
- [引用方块函数：`$`](reference/zh.md)：引用数据包中定义的方块函数。

### 函数语法

- [`checkerboard(<方块函数> [权重] ...)`](checkerboard/zh.md)：棋盘格的图案。
- [`checkerboard-tag(<方块函数> [权重] ...)`](checkerboard-tag/zh.md)：以拥有方块标签的方块为内容的棋盘格图案。
- [`dry([方块函数])`](dry/zh.md)：除去当前方块的水，或者先应用方块函数然后再除去水。
- [`filter(<方块函数 1>, <方块谓词>, [方块函数 2])`](filter/zh.md)：先计算出方块函数 1 的结果，如果方块函数 1 的结果符合方块谓词，那么直接应用，否则计算方块函数 2 或者不操作。
- [`idcontain(<正则表达式>)`](idcontain/zh.md)：随机选择一个 ID 符合指定的正则表达式的方块。
- [`idreplace(<正则表达式>)`](idreplace/zh.md)：对方块 ID 进行替换，如果替换后的方块 ID 存在，则应用它。
- [`if(<方块谓词>, <方块函数 1>, [方块函数 2])`](if/zh.md)：检测原先的方块是否符合指定的方块谓词，如果符合，则使用方块函数 1，否则使用方块函数 2 或者不操作。
- [`ifs(<方块谓词 1>, <方块函数 1a>, [方块函数 1b]; ...)`](ifs/zh.md)：检测多个方块谓词。
- [`mirror(<方向>)`](mirror/zh.md)：对当前方块进行镜像。
- [`noise(<方块函数> [权重], ...; ...)`](noise/zh.md)：噪声。
- [`overlay([方块函数], ...)`](overlay/zh.md)：依次应用多个方块函数。
- [`pick(<方块函数>, ...)`](pick/zh.md)：随机选择方块函数进行应用。
- [`postprocess<方向> ...`](postprocess/zh.md)：对方块进行后处理，例如修复栅栏与墙的连接问题。
- [`random()`](random/zh.md)：随机选择方块状态。和上面的随机方块函数相同，但支持指定种子。
- [`rotate(<方向>)`](rotate/zh.md)：对当前方块进行旋转。
- [`stonecut([方块函数])`](stonecut/zh.md)：对当前方块或者指定方向函数计算的结果进行切石操作。如果可能有多个不同的切石结果，则随机选择一个切石结果。

## 数据结构

- `type`：字符串。方块函数的类型 ID。
- 各类型各自的字段。

方块函数共有以下类型（均省略了 `enhanced_commands` 命名空间）：

- [`simple`](simple/zh.md)
- [`property_names`](property_names/zh.md)
- [`nbt`](nbt/zh.md)
- [`properties_nbt_combination`](property_nbt_combination/zh.md)
- [`empty`](empty/zh.md)
- [`random`](random/zh.md)
- [`tag`](tag/zh.md)
- [`use_original`](use_original/zh.md)
- [`checkerboard`](checkerboard/zh.md)
- [`checkerbard-tag`](checkerboard-tag/zh.md)
- [`conditional`](if/zh.md)
- [`conditions`](ifs/zh.md)
- [`dry`](dry/zh.md)
- [`filter`](filter/zh.md)
- [`id_contain`](idcontain/zh.md)
- [`id_replace`](idreplace/zh.md)
- [`mirror`](mirror/zh.md)
- [`noise`](noise/zh.md)
- [`overlay`](overlay/zh.md)
- [`pick`](pick/zh.md)
- [`post_process`](postprocess/zh.md)
- [`reference`](reference/zh.md)
- [`rotate`](rotate/zh.md)
- [`stonecut`](stonecut/zh.md)