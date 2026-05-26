# `/nbt`: Operate on NBT as you want

This [command](../en.md) can be used to query or modify NBT data, including NBT data in blocks, entities and storage.

## Syntax

- `/nbt get <source> [path] [keyword args: scale | concentration_type]`: Get NBT from a specified source.
- `/nbt set <target> <path> <NBT function>`: Set the NBT at a specified target to the result of the specified NBT function.
- `/nbt merge <target> <NBT function>`: Merge the result of the specified NBT into the specified target's NBT.
- `/nbt replace <target> <path> <NBT predicate> <NBT function>`: Apply function to any part that match the predicate of the NBT at the specified path of the specified targets.
- `/nbt string_replace <target> <path> <target string> <replacement string> [keyword args: lenient | recursive | affect_only]`: Apply string replacement on the NBT at the specified path of the specified targets.
- `/nbt regex_replace <target> <path> <regex> <replacement> [keyword args: lenient | recursive | affect_only]`: Apply regular expression replacement on the NBT at the specified path of the specified targets.
- `/nbt substring <target> <path> <start index> [end index] [keyword args: lenient | affect_only]`: Cut a substring from the NBT at the specified path of the specified target. Both the start index and the end index are integers. If the end index is not provided, it is by default cut until the end of the string.
- `/nbt remove <target> <path>`: Remove the part of the specified path from the specified target's NBT.

## Parameters

### `<source>`

[NBT source](/documents/arguments/nbt_source/en.md).

### `<target>`

[NBT target](/documents/arguments/nbt_target/en.md).

### `<path>`

NBT path, same as vanilla usage.

### `<NBT predicate>`

[NBT predicate](/documents/arguments/nbt_predicate/en.md).

### `<NBT function>`

[NBT function](/documents/arguments/nbt_function/en.md).

### Keyword arguments

* `lenient`: Boolean, if `true`, then when the string index is invalid, or the end index is before the start index, no replacement is executed while leaving no error.
* `recursive`: Boolean, if `true`, the replacement will be executed recursively, which means all elements in the compounds and lists will be executed replacement.
* `affect_only`: Block predicate. If set, only NBTs of blocks that match the block predicate will be affected.

## Examples

- `/nbt get entity @s SelectedItemSlot`: Get my selected item slot number.
- `/nbt set entity @e[type=sheep] Age -1000`: Set all sheep to baby entities.
- `/nbt merge entity @e[r=5] {CustomName:'"Dinnerbone"'}`: Name all entities within the range 5 blocks to Dinnerbone。
- `/nbt replace entity @s Inventory {id:"minecraft:diamond"} {count:64}`: Set the count of all diamonds in my inventory to 64.
- `/nbt string_replace block sphere(20) Items red blue recursive=true`: Within the block entity data of all block entities within 20 blocks, replace string `red` with `blue` (for example, red wool in the chest will be replaced with blue wool).
- `/nbt remove entity @s Inventory`: Clear my own inventory.

## See also

- [NBT function `replace()`](/documents/arguments/nbt_function/replaceblocks/en.md)
- [NBT function `string_replace()`](/documents/arguments/nbt_function/string_replace/en.md)
- [NBT function `regex_replace()`](/documents/arguments/nbt_function/regex_replace/en.md)
- [NBT function `substring()`](/documents/arguments/nbt_function/substring/en.md)