# `regex_replace()` function: replacing regex in the NBT

The [NBT function](../en.md) replaced regular expressions in the NBT.

> **Warning:** It may be risky to replace NBTs, because you may affect many values at once, and it is usually not easy to undo the changes. Before you execute replacements, especially replacements that may affect many targets and many values, check carefully. It is also recommended to execute replacement in a smaller range (to fewer blocks, fewer entities, etc.), and make sure the replacement is executed correctly as expected before executing in larger ranges.

## Syntax

- `regex_replace(<pattern>, <replacement>)`
- `regex_replace(<pattern>, <replacement>, [keyword args: recursive | lenient | original])`

## Parameters

### `<pattern>`

Regular expression. Parts matching the regular expression will be replaced. If the regular expression is incorrect, fails to parse.

If multiple parts in the content to be replaced are found to match the regular expression, all of them will be replaced. If there is nothing matching the regular expression, exceptions will not be thrown.

### `<replacement>`

String. Supports group syntax like `$1`. If the group syntax is invalid, the argument is parsed normally, but may fail to run normally.

### Keyword arguments

#### `recursive`

Boolean value, indicating whether to replace recursively. False by default.

- If false, when the NBT to be replaced is a string, it is replaced normally, while when the NBT to be replaced is other types, replacement will not be executed, and exceptions will be thrown, unless `lenient` is true.
- If true, when the NBT to be replaced is a string, it is replaced normally. When the replaced NBT is a list or compound, the strings that the list or compound contains (including recursive inclusion; for compounds, only values instead of keys will be replaced) will be executed replacements. For other types, replacement is not executed.

#### `lenient`

Boolean value, indicating whether to avoid throwing exceptions. False by default.

- If false, when the replaced NBT is not a string and `recursive` is not true, exceptions will be thrown. If the replacement contains groups that do not exist in the regular expression, or contains invalid group syntax, exceptions will be thrown.
- If true, none of the situations above causes exceptions. But when the replaced NBT is null, exceptions will still be thrown.

#### `original`

NBT function, optional. Indicating the original value before replacement.

- If not specified, the replaced NBT is the argument during NBT function invocation.
- If specified, the replaced NBT is the value of the argument. This argument is an NBT function, which will be invocated before replacing the result of the function.

## Examples

- `regex_replace("[ab]", "<$1>")`:
    - Applied to `"cat"`: Returns `"c<a>t"`.
    - Applied to `"sky"`: Returns `"sky"` (unchanged).
    - Applied to `1b`: Error.
    - Applied to `[cat, sky]`: Error.
    - Applied to `{a: cat, b: sky, c: [bat, chat]}`: Error.
- `regex_replace("[ab]", "<$1>", recursive = true)`:
    - Applied to `"cat"`: Returns `"c<a>t"`.
    - Applied to `"sky"`: Returns `"sky"` (unchanged).
    - Applied to `1b`: Error.
    - Applied to `[cat, sky]`: Returns `['c<a>t', sky]`.
    - Applied to `{a: cat, b: sky, c: [bat, chat]}`: Returns `{a: 'c<a>t', b: sky, c: ['<b><a>t', 'ch<a>t']}`.
- `regex_replace("[ab]", "<$1>", lenient = true)`:
    - Applied to `"cat"`: Returns `"c<a>t"`.
    - Applied to `"sky"`: Returns `"sky"` (unchanged).
    - Applied to `1b`: Unchanged.
    - Applied to `[cat, sky]`: Unchanged.
    - Applied to `{a: cat, b: sky, c: [bat, chat]}`: Unchanged.
- `regex_replace("[mn]", '$1')`:
    - Applied to `"mine"`: Error, because group 1 does not exist in the regular expression.
- `regex_replace("[mn]", '$1', lenient = true)`:
    - Applied to `"mine"`: Unchanged (group 1 does not exist in the regular expression, so the replacement does not happen).
- `regex_replace("[oe]", "<$1>", original = "fromage")`:
    - Applied to any NBT: Returns `"fr<o>m<a>ge"`.

## Data format

* **target**: String.
* **replacement**: String.
* **recursive**: Boolean, false by default.
* **lenient**: Boolean, false by default.
* **original**: Compound tag, indicating an NBT function. Optional.

## 参见

- [`replace()` function](../replace/en.md): Replacing NBTs that match the predicate.
- [`string_replace()` function](../string_replace/en.md): Replacing texts simply.