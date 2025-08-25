# `idcontain()`: Match Regex Randomly

This [block function](../en.md) randomly selects a block whose ID match the specified regular expression.

## Syntax

- `idcontain(<regex>)`
- `idcontain(<regex>, seed = <seed>)`

## Parameters

### `<regex>`

The regular expression may not require quotation marks even if it contains characters like `$`, `^`, `?`. But if it contains parentheses, quotation marks or other characters, quotation marks are still needed.

### `<seed>`

Long integer. The seed used to randomly select blocks.

## Examples

- `idcontain(_terracotta$)`: Randomly selects blocks whose ID ends with "terracotta", which means terracotta and glazed terracotta.
- `idcontain(red)`: Randomly selects blocks whose ID contains "red".
- `idcontain(stone, seed = 3)`: Randomly selects blocks whose ID contains "stone", with the random seed 3.
- `idcontain(*)`: Invalid regex, which cannot be parsed.

## See also

- [The block predicate `idcontain()`](../../block_predicate/idcontain/en.md)