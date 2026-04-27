# `overlay()`

This [NBT function](../en.md) is used to apply multiple NBT functions.

## Syntax

`overlay([NBT function], ...)`

There may be no NBT functions, which means to return the parameter directly without operation, but if the parameter is null at this time, an error will be thrown.

## Examples

- `overlay(string_replace(red, green), string_replace(concrete, terracotta))`: In the string, first replace `"red"` with `"green"`, and then in the string replace `"concrete"` with `"terracotta"`. Identical to `string_replace(red, green)|string_replace(concrete, terracotta)`.

## Data structure

- `type`: At this time `enhanced_commands:overlay`.
- `functions`: List.
    - NBT function.

## See also

- [`overlay()` block function](../../block_function/overlay/en.md)
- [`overlay()` item](../../item_function/overlay/en.md)