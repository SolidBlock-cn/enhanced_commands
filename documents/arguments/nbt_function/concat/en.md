# `concat()` function: concatenating multiple strings

The [NBT function](../en.md) can be used to concatenate multiple strings. It can concatenate strings and numbers; numbers will be converted into strings (without the datatype suffix). If there are any values of other types in the concatenation, exceptions will be thrown.

## Syntax

- Syntax 1：`concat(<NBT function 1>, <NBT function 2> ...; delimiter = [delimiter])`: Direct concatenate the return values of multiple NBT functions.
- Syntax 2：`concat(* <NBT function>; delimiter = [delimiter])`: If the return value of this NBT function is a list, the elements in the list will be concatenated. If not a list, exceptions will be thrown.

## Parameters

### `<NBT function> ...` (syntax 1)

The [NBT function](../en.md) to be concatenated. The amount is not limited. In syntax 1, each NBT function when being applied, takes the param of the whole function as param. If one of the NBT functions is not a string or number, exceptions will be thrown.

### `<NBT function>` (syntax 2)

The [NBT function](../en.md) to be concatenated as a list. If the return value of the NBT function is not a list, exceptions will be thrown. If it is a list, the elements in the list will be concatenated. If one of the elements is not a string or number, exceptions will be thrown.

### `delimiter = [delimiter]`

[NBT function](../en.md). The string form of the return value of it will be used as a delimiter to connect the elements. If the return values of the NBT function is not a string or number, exceptions will be thrown.

## Examples

### Syntax 1 examples

- `concat(a, b, c)`: Return string `"abc"`.
- `concat(a, 1, 5)`: Return string `a15`.
- `concat(1L, 2s)`: Return string `12`. The concatenation is not with datatype suffix.
- `concat(true, false)`: Return string `10`, as both `true` and `false` are treated as `1b` and `0b` actually.
- `concat(1, [], 2)`: Cannot execute, as the list cannot be used to concatenate.
- `concat(1, {}, a)`: Cannot execute, as the compound cannot be used to concatenate.

### Syntax 2 examples

- `concat(* 1)`: Cannot execute, as 1 is not a list.
- `concat(* {})`: Cannot execute ,as `{}` is not a list.
- `concat(* [1, 2, 3])`: Return `123`.
- `concat(* [a, 1, 5])`: Return `a`, as when the NBT list function `[a, 1, 5]` is being applied, the latter two elements 1 and 5 cannot be added to the list as datatype mismatch, so the function only concatenates within list `[a]`.

## Data structure

* **type**: String, currently `enhanced_commands:concat`.
* **flatten**: Boolean. Whether the NBT function is flattening.
* **elements**: List. Only when `flatten: false`. The NBT functions.
    * Compound. An NBT function.
* **element**: Compound. Only when `flatten: true`. The NBT function.
* **delimiter**: Compound. The NBT function.