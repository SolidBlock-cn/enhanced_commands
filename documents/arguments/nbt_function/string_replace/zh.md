# `string_replace()` 函数：替换 NBT 中的字符串

此 [NBT 函数](../zh.md) 可对 NBT 中的字符串进行替换。

> 警告：替换 NBT 可能会有风险，因为你可能一次影响许多值，且通常不好撤销。在执行替换前，尤其是在执行可能影响许多目标、许多值的替换前，请仔细检查。也建议先在更小范围内（更少方块、更少实体等）执行替换，并确保替换正确执行了再在更大范围内执行。

## 语法

- `string_replace(<查找内容>, <替换成的内容>)`
- `string_replace(<查找内容>, <替换成的内容>; [关键字参数：recursive | lenient | original])`

## 参数

### `<查找内容>`

字符串。在字符串中找到此字符串的内容，并进行替换。

如果被替换的内容中找到多个，则会全部替换。如果没有找到，也不会抛出异常。

### `<替换成的内容>`

字符串。

### 关键字参数

#### `recursive`

布尔值，表示是否递归替换。默认为 false。

- 若为 false，则当被替换的 NBT 是字符串时，正常替换，而当被替换的 NBT 是其他类型时，不执行替换，而是抛出异常（除非 `lenient` 为 true）。
- 若为 true，则当被替换的 NBT 是字符串时，正常替换；当被替换的 NBT 是列表或复合标签中，会对该列表或复合标签中所包含的字符串（包括嵌套包含的，其中复合标签只替换值，不替换键）进行替换。如果是其他类型，则不执行替换。

#### `lenient`

布尔值，表示是否避免抛出异常。默认为 false。

- 若为 false，则当被替换的 NBT 不是字符串且 `recursive` 不为 true 时，抛出异常。
- 若为 true，则上述情况下均不抛出异常。但是当被替换的 NBT 是 null 时，仍会抛出异常。

#### `original`

NBT 函数，可选。表示替换前的原始值。

- 若未指定，则被替换的 NBT 为该 NBT 函数运行时的参数。
- 若指定，则被替换的 NBT 为该参数的值。该参数是 NBT 函数，会先运行此 NBT 函数，再对该 NBT 函数的结果进行替换。

## 示例

- `string_replace("a", "u")`：
    - 应用于 `"cat"`：返回 `"cut"`。
    - 应用于 `"sky"`：返回 `"sky"`（不变）。
    - 应用于 `1b`：错误。
    - 应用于 `[cat, sky]`：错误。
    - 应用于 `{a: cat, b: sky, c: [bat, chat]}`：错误。
- `string_replace("a", "u"; recursive = true)`：
    - 应用于 `"cat"`：返回 `"cut"`。
    - 应用于 `"sky"`：返回 `"sky"`（不变）。
    - 应用于 `1b`：错误。
    - 应用于 `[cat, sky]`：返回 `['cut', sky]`。
    - 应用于 `{a: cat, b: sky, c: [bat, chat]}`：返回 `{a: 'cut', b: sky, c: ['but', 'chut']}`。
- `string_replace("a", "u"; lenient = true)`：
    - 应用于 `"cat"`：返回 `"cut"`。
    - 应用于 `"sky"`：返回 `"sky"`（不变）。
    - 应用于 `1b`：不变。
    - 应用于 `[cat, sky]`：不变。
    - 应用于 `{a: cat, b: sky, c: [bat, chat]}`：不变。
- `string_replace("e", "es"; original = "fromage")`：
    - 应用于任何 NBT：返回 `"fromages"`。

以下是一些具体命令的示例：

- `/nbt set block ~ ~ ~ {} string_replace(red, blue; recursive = true)`：将当前位置的方块的 NBT 中的“red”替换为“blue”；例如，如果这个方块是告示牌，牌上有文本“This is a red text”，并被染成红色，则替换后文本会变成蓝色，内容也会变成“This is a blue text”。
- `/nbt set block ~ ~-1 ~ Command string_replace(player, zombie)`：将下方的方块的 NBT 中的 `Command` 中的 player 替换为 zombie；例如，如果这个方块是命令方块，其命令为 `/testfor entity @e[type=player]`，则替换后为 `/testfor entity @e[type=zombie]`。
- `/nbt set entity @s Inventory string_replace(cherry, oak; recursive = true)`：将玩家自身物品栏中的樱花相关物品替换为橡木相关物品。

## 数据结构

* **target**：字符串。
* **replacement**：字符串。
* **recursive**：布尔值，默认为 false。
* **lenient**：布尔值，默认为 false。
* **original**：复合标签。表示 NBT 函数，可选。

## 参见

- [`replace()` 函数](../replace/zh.md)：替换符合谓词的 NBT。
- [`regex_replace()` 函数](../regex_replace/zh.md)：替换正则表达式。