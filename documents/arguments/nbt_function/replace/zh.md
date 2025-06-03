# `replace()`：替换 NBT 中的值

此 [NBT 函数](../zh.md)可用于对 NBT 中的符合指定 NBT 谓词的值进行替换，替换的方式就是应用指定的 NBT 函数。

如果被替换的内容自身即符合这个谓词，则直接应用指定的 NBT 函数。如果被替换的内容自身不符合这一谓词，但是其包含（包括递归包含）的值符合这一谓词，则这些值都会被应用 NBT 函数。复合标签、列表都会递归应用，但是如果复合标签、列表本身即符合谓词，则不再对该复合标签或列表进行进一步的递归替换。

此函数并非用于查找并替换字符串中的部分内容，如需对字符串进行查找并替换，请使用 [`string_replace()` 函数](../string_replace/zh.md)或 [`regex_replace()` 函数](../regex_replace/zh.md)。

## 语法

- `replace(<NBT 谓词>, <NBT 函数>)`

## 参数

### `<NBT 谓词>`

[NBT 谓词](../../nbt_predicate/zh.md)。将会检测被替换内容中符合此谓词的值。

### `<NBT 函数>`

[NBT 函数](../zh.md)。对于符合此谓词的值应用此函数。

## 示例

- `replace(a, b)`：将字符串 `"a"` 替换成字符串 `"b"`。
- `replace(1b, 2b)`：将数字 `1b` 替换成 `2b`。
- `replace({key=value}, {key2=value2})`：将符合 `{key = value}` 的复合标签的 `key2` 设置为 `value2`。

以下是具体命令中的一些示例：

- `/nbt set block sphere(5) {} replace({color: black}, {color: white})`：将半径 5 个方块内的告示牌的黑色文本设置为白色，两面都会影响。
- `/nbt set entity @s Inventory replace({count: 1}, {count: 99})`：将玩家物品栏中的个数为 1 个的物品设置为 99 个。

## 数据结构

* **predicate**：复合标签。
* **function**：复合标签。

## 参见

- [`regex_replace()` 函数](../regex_replace/zh.md)：替换正则表达式。
- [`string_replace()` 函数](../string_replace/zh.md)：简单替换文本。