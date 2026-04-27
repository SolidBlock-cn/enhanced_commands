# `pick()`: Choose Block Functions Randomly

This [block function](../en.md) is used to select from multiple block functions. You can specify a weight for each block function, which is by default 1.

## Syntax

- `pick(<block function> [weight], ...)`
- `pick(<block function> [weight], ...; seed = <seed>)`

## Parameters

### `<block function>`

At least one [block function](../en.md) to be applied.

### `[weight]`

Floating point number, between 0 and 1. Relative probability each block function can be selected. Block functions with higher weight are more likely to be chosen. If omitted, by default 1. Can be 0, but the weights of all block functions cannot sum to 0.

### `<seed>`

Long integer. The seed to determine the block function to be used for each position.

## Examples

- `pick(white_wool, blue_wool)`: Generates white wool or blue wool randomly, with equal probability. Identical to `white_wool|blue_wool`.
- `pick(white_wool, blue_wool; seed = 3)`: Generates white wool or blue wool randomly, with equal probability. The random seed is 3, so if you apply this block function on the same place multiple times, you will get the same result.
- `pick(white_wool 3, blue_wool 2)`: Generates white wool and blue wool randomly; probability for white wool is 60%, for blue wool is 40%.
- `pick(white_wool 3, blue_wool)`: Generates white wool and blue wool randomly; probability for white wool is 75%, and for blue wool is 25%.
- `pick(white_wool, blue_wool 0)`: Generates white wool only, as the probability for blue wool is 0.
- `pick(white_wool 0, blue_wool 0)`: Invalid block function, as the sum of weights is 0.

## See also

- [`pick()` item function](../../item_function/pick/en.md)
- [`pick()` NBT function](../../nbt_function/pick/en.md)