# `pos()` function: using coordinates in the NBT

The [NBT function](../en.md) returns a coordinate in the form of array. Although you can directly write arrays, this NBT function supports various complicated coordinate syntax, including relatives coordinate and locals coordinate.

## Syntax

- `pos(<pos>)`

## Parameters

### `<pos>`

[Coordinate](../../pos/en.md). Supports relative coordinates and local coordinates.

## Examples

- `pos(1 5 4)`
- `pos(~ ~ ~3)`
- `pos(^ ^ ^2)`