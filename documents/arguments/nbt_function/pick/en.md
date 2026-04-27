# `pick()`: Choose an NBT function randomly

This [NBT function](../en.md) is used to select from multiple block functions. You can specify a weight for each block function, which is by default 1.

## Syntax

- `pick(<NBT function> [weight], ...)`

## Arguments

### `<NBT function>`

At least one [NBT function](../en.md) to be applied.

### `[weight]`

Floating point number, between 0 and 1. Relative probability each NBT function can be selected. NBT functions with higher weight are more likely to be chosen. If omitted, by default 1. Can be 0, but the weights of all NBT functions cannot sum to 0.

## Examples

- `pick(123, 456)`: Return one of the integers `123` or `456` randomly with equal probability. Identical to `123|456`.
- `pick(123 3, 456 2)`: Return one of the integers `123` or `456`; probability to return integer `123` is 60%, for`456` is 40%.
- `pick(123 0, 456 0)`: Invalid block function, as the sum of weights is 0.

## Data structure

- `type`: Currently `enhanced_commands:pick`.
- `functions`: Compound. List of NBT functions to be randomly chosen.
    - `weighted`: Boolean.
    - `elements`: List, only exists when `weighted` is false.
        - NBT function, an entry to be chosen randomly.
    - `entries`: List, only exists when `weighted` is true.
        - Compound. An entry.
            - `element`: NBT function.
            - `weighted`: Double-precision floating-point number.

## See also

- [`pick()` block function](../../block_function/pick/en.md)
- [`pick()` item function](../../item_function/pick/en.md)