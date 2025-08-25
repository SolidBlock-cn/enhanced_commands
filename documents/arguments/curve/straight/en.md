# `straight()`

A straight line segment within two points.

The algorithm when iterating block positions is special.

## Syntax

- `straight(<pos 1>, <pos 2>)`
- `straight(from <pos 1> to <pos 2>)`

The two syntaxes above are identical.

## Parameters

### `<pos 1>`, `<pos 2>`

The coordinate of the line segment's end points, supporting relative coordinates and local coordinates.

## Examples

- `straight(1 2 3, 4 5 6)`: Line segment from (1, 2, 3) to (4, 5, 6).
- `straight(from 1 2 3 to 4 5 6)`: Identical to the previous example.
- `straight(~~~, ~~5~)`: Line segment from the current position to the position 5 blocks above.
- `straight(^^^, ^^^5)`: Line segment from the current position to 5 blocks forwards in the sight direction.

## Data structure

- `type`: Currently `"enhanced_commands:straight"`.
- `from`: Coordinate.
- `to`: Coordinate.