# `/nbt`：随心所欲地操作 NBT

此[命令](../zh.md)可用于查询或修改 NBT 数据，包括方块、实体和存储的 NBT 数据。

## 语法

- `/nbt get <来源> [路径] [关键字参数：scale | concentration_type]`：获取指定来源的 NBT：
- `/nbt set <目标> <路径> <NBT 函数>`：设置指定目标的 NBT 为指定的 NBT 函数的结果。
- `/nbt merge <目标> <NBT 函数>`：将指定 NBT 函数的结果与指定目标的 NBT 合并。
- `/nbt replace <目标> <路径> <NBT 谓词> <NBT 函数>`：将指定目标、指定路径的 NBT 中的任何符合谓词的部分均应用函数。
- `/nbt string_replace <目标> <路径> <目标字符串> <替换字符串> [关键字参数：lenient | recursive | affect_only]`：对指定目标、指定路径的 NBT 进行字符串替换。
- `/nbt regex_replace <目标> <路径> <正则表达式> <替换内容> [关键字参数：lenient | recursive | affect_only]`：对指定目标、指定路径的 NBT 进行正则表达式替换。
- `/nbt substring <目标> <路径> <起始索引> [结束索引] [关键字参数：lenient | affect_only]`：对指定目标、指定路径的 NBT 截取子字符串。起始索引和结束索引均为整数。结束索引未指定，则默认为截取到字符串末尾。
- `/nbt remove <目标> <路径>`：将指定目标的 NBT 的指定路径的部分移除。

## 参数

### `<来源>`

[NBT 来源](/documents/arguments/nbt_source/zh.md)。

### `<目标>`

[NBT 目标](/documents/arguments/nbt_target/zh.md)。

### `<路径>`

NBT 路径，和原版用法一致。

### `<NBT 谓词>`

[NBT 谓词](/documents/arguments/nbt_predicate/zh.md)。

### `<NBT 函数>`

[NBT 函数](/documents/arguments/nbt_function/zh.md)。

### 关键字参数

* `lenient`：布尔值，如果为 `true`，则当字符串索引无效，或者结束索引在开始索引之前时，不执行替换，但也不会提示错误。
* `recursive`：布尔值，如果为 `true`，那么替换将会递归运行，也就是说复合标签、列表中的所有元素，都会执行替换。
* `affect_only`：方块谓词，如果设置，则只有符合该方块谓词的方块的 NBT 才会受到影响。

## 示例

- `/nbt get entity @s SelectedItemSlot`：获取自己的物品栏位置。
- `/nbt set entity @e[type=sheep] Age -1000`：将所有的羊设置为幼年实体。
- `/nbt merge entity @e[r=5] {CustomName:'"Dinnerbone"'}`：将距离 5 个方块范围内的实体的名字设置为 Dinnerbone。
- `/nbt replace entity @s Inventory {id:"minecraft:diamond"} {count:64}`：将自己物品栏里的所有钻石设置为 64 个。
- `/nbt string_replace block sphere(20) Items red blue recursive=true`：将附近 20 个方块范围内的所有方块实体数据中的 `red` 替换为 `blue`（例如箱子中的红色羊毛将替换为蓝色羊毛）。
- `/nbt remove entity @s Inventory`：清除自己的物品栏。

## 参见

- [NBT 函数 `replace()`](/documents/arguments/nbt_function/replaceblocks/zh.md)
- [NBT 函数 `string_replace()`](/documents/arguments/nbt_function/string_replace/zh.md)
- [NBT 函数 `regex_replace()`](/documents/arguments/nbt_function/regex_replace/zh.md)
- [NBT 函数 `substring()`](/documents/arguments/nbt_function/substring/zh.md)