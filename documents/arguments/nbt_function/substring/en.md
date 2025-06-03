# `substring()` function: get a substring

This [NBT function](../en.md) is used to get a substring.

## Syntax

- `substring(<start_index>, [end_index])`
- `substring(<start_index>, [end_index]; [keyword arguments: lenient | original])`

## Parameters

### `<start_index>`

Integer. Indicating the start index of string. Supports negative index.

### `[end_index]`

Integer. Indicating the end index of string. Supports negative index.

If not specified, get the substring until the end of the string.

### Keyword arguments

#### `lenient`

Boolean, by default false.

- If false, if the start index or end index is invalid (greater than the actual length of string, or lower than the opposite number of the string length), or the actual index of start index is greater than the actual index of end index, exceptions will be thrown. If the NBT to get substring is not a string, exceptions will also be thrown.
- If true, none of the situations above causes an exception, and the original value will be returned directly, unless the NBT to get substring is null.

#### `original`

NBT function, optional. Indicating the original value.

- If not specified, the replaced NBT is the argument during NBT function invocation.
- If specified, the replaced NBT is the value of the argument. This argument is an NBT function, which will be invocated before replacing the result of the function.

## Examples

- `substring(2, 4)`: Get the substring from position 3 to position 4.
    - Applied to `"apple"`: Returns `"pl"`.
    - Applied to `"sandwich"`: Returns `"nd"`.
    - Applied to `"mc"`: Error.
    - Applied to `1234`: Error.
    - Applied to `["a", "b"]`: Error.
- `substring(2)`: Get the substring from position 3 to the end.
    - Applied to `"apple"`: Returns `"ple"`.
    - Applied to `"sandwich"`: Returns `"ndwich"`，
    - Applied to `"mc"`: Returns an empty string.
- `substring(3, -3)`: Get the substring from position 4 to position 4 from the end.
    - Applied to `"apple"`: Error.
    - Applied to `"sandwich"`: Returns `"dw"`.
    - Applied to `"mc"`: Error.
- `substring(5, 3)`: Error when applied to any string.
- `substring(-5, -3)`: Get the substring from position 5 from the end to position 4 from the end.
    - Applied to `"apple"`: Returns `"ap"`.
    - Applied to `"sandwich"`: Returns `"dw"`.
    - Applied to `"mc"`: Error.
- `substring(-3, -5)`: Error when applied to any string.
- `substring(1, 3; original = "cake")`: Returns `"ak"` for any NBT.
- `substring(2, 4; lenient = true)`:
    - Applied to `"apple"`: Returns `"pl"`.
    - Applied to `"sandwich"`: Returns `"nd"`.
    - Applied to `"mc"`: Returns `"mc"` (unchanged).
    - Applied to `1234`: Returns `1234` (unchanged).
    - Applied to `["a", "b"]`: Returns `["a", "b"]` (unchanged).

## Data format

- **start_index**: Integer.
- **end_index**: Integer, optional.
- **lenient**: Boolean, optional, false by default.
- **original**: Compound tag, indicating an NBT function, optional.