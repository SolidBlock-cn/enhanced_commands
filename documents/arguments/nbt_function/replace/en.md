# `replace()`: replacing values in the NBT

This [NBT function](../en.md) can be used to replace values that match the specified NBT predicate within the NBT, by applying the specified NBT function.

If the NBT to be replaced itself match the predicate, the NBT function will be applied directly. If the NBT to be replaced itself does not match the predicate, but it includes (including recursive inclusion) values that match the predicate, these values will be applied the NBT function. Compounds and lists will be applied recursively, but if the compound or list itself matches the predicate, the compound or list will not be further recursively replaced.

The function is not used to find and replace some contents in the string. To find and replace in the string, please use [`string_replace()` function](../string_replace/en.md) or [`regex_replace()` function](../regex_replace/en.md).

> **Warning:** It may be risky to replace NBTs, because you may affect many values at once, and it is usually not easy to undo the changes. Before you execute replacements, especially replacements that may affect many targets and many values, check carefully. It is also recommended to execute replacement in a smaller range (to fewer blocks, fewer entities, etc.), and make sure the replacement is executed correctly as expected before executing in larger ranges.

## Syntax

- `replace(<NBT predicate>, <NBT function>)`

## Parameters

### `<NBT predicate>`

[NBT predicate](../../nbt_predicate/en.md). In the NBT to be replaced, values that match this predicate will be tested.

### `<NBT function>`

[NBT function](../en.md). This function will be applied to values that match this predicate.

## Examples

- `replace(a, b)`: Replace string `"a"` with string `"b"`.
- `replace(1b, 2b)`: Replace number `1b` with `2b`.
- `replace({key=value}, {key2=value2})`: Replace the compounds that match `{key = value}` by setting the value of field `key2` to `value2`.

Here are some examples in the practical commands:

- `/nbt set block sphere(5) {} replace({color: black}, {color: white})`: Change the black texts of sign blocks within 5 blocks to white; both sides of the sign will be influenced.
- `/nbt set entity @s Inventory replace({count: 1}, {count: 99})`：将玩家物品栏中的个数为 1 个的物品设置为 99 个。

## Data structure

* **predicate**: Compound tag.
* **function**: Compound tag.

## See also

- [`regex_replace()` function](../regex_replace/zh.md): Replacing regular expressions.
- [`string_replace()` function](../string_replace/zh.md): Replacing texts simply.