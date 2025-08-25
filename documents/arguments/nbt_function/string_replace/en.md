# `string_replace()` function: Replacing strings in the NBT

The [NBT function](../en.md) replaces strings in the NBT.

> **Warning:** It may be risky to replace NBTs, because you may affect many values at once, and it is usually not easy to undo the changes. Before you execute replacements, especially replacements that may affect many targets and many values, check carefully. It is also recommended to execute replacement in a smaller range (to fewer blocks, fewer entities, etc.), and make sure the replacement is executed correctly as expected before executing in larger ranges.

## Syntax

- `string_replace(<targetString>, <replacement>)`
- `string_replace(<targetString>, <replacement>, [keyword_args: recursive | lenient | original])`

## Parameters

### `<targetString>`

String. Find the target string in the string, and replace them.

If there are multiple strings found in the content to be replaced, all of them will be replaced. If none are found, exceptions will not be thrown.

### `<replacement>`

String.

### Keyword arguments

#### `recursive`

Boolean value, indicating whether to replace recursively. False by default.

- If false, when the NBT to be replaced is a string, it is replaced normally, while when the NBT to be replaced is other types, replacement will not be executed, and exceptions will be thrown, unless `lenient` is true.
- If true, when the NBT to be replaced is a string, it is replaced normally. When the replaced NBT is a list or compound, the strings that the list or compound contains (including recursive inclusion; for compounds, only values instead of keys will be replaced) will be executed replacements. For other types, replacement is not executed.

#### `lenient`

Boolean value, indicating whether to avoid throwing exceptions. False by default.

- If false, when the replaced NBT is not a string and `recursive` is not true, exceptions will be thrown.
- If true, none of the situations above causes exceptions. But when the replaced NBT is null, exceptions will still be thrown.

#### `original`

NBT function, optional. Indicating the original value before replacement.

- If not specified, the replaced NBT is the argument during NBT function invocation.
- If specified, the replaced NBT is the value of the argument. This argument is an NBT function, which will be invocated before replacing the result of the function.

## Examples

- `string_replace("a", "u")`:
    - Applied to `"cat"`: Returns `"cut"`.
    - Applied to `"sky"`: Returns `"sky"` (unchanged).
    - Applied to `1b`: Error.
    - Applied to `[cat, sky]`: Error.
    - Applied to `{a: cat, b: sky, c: [bat, chat]}`: Error.
- `string_replace("a", "u", recursive = true)`:
    - Applied to `"cat"`: Returns `"cut"`.
    - Applied to `"sky"`: Returns `"sky"` (unchanged).
    - Applied to `1b`: Error.
    - Applied to `[cat, sky]`: Returns `['cut', sky]`.
    - Applied to `{a: cat, b: sky, c: [bat, chat]}`: Returns `{a: 'cut', b: sky, c: ['but', 'chut']}`.
- `string_replace("a", "u", lenient = true)`:
    - Applied to `"cat"`: Returns `"cut"`.
    - Applied to `"sky"`: Returns `"sky"` (unchanged).
    - Applied to `1b`: Unchanged.
    - Applied to `[cat, sky]`: Unchanged.
    - Applied to `{a: cat, b: sky, c: [bat, chat]}`: Unchanged.
- `string_replace("e", "es", original = "fromage")`:
    - Applied to any NBT: Returns `"fromages"`.

The following are some examples in actual commands:

- `/nbt set block ~ ~ ~ {} string_replace(red, blue, recursive = true)`: Replace world "red" with "blue" within the NBT of the current place. For example, if it is a sign, and the sign has a text that reads "This is a red text", and died red, the replaced text will be blue, and the content is replaced with "This is a blue text".
- `/nbt set block ~ ~-1 ~ Command string_replace(player, zombie)`: Replaced the word "player" in the value of `Command` of the NBT of the block below with "zombie". For example, if the block is a command block with command `/testfor entity @e[type=player]`, it will be replaced with `/testfor entity @e[type=zombie]`.
- `/nbt set entity @s Inventory string_replace(cherry, oak, recursive = true)`: Replace cherry-related items in the player itself's inventory to oak-related items.

## Data format

* **target**: String.
* **replacement**: String.
* **recursive**: Boolean, false by default.
* **lenient**: Boolean, false by default.
* **original**: Compound tag, indicating an NBT function. Optional.

## See also

- [`replace()` function](../replace/en.md): Replacing NBTs that match the predicate.
- [`regex_replace()` function](../regex_replace/en.md): Replacing regular expressions.