# `*` and `rand()`: Random Block

This [block function](../en.md) returns a block randomly, and adds block state properties randomly.

## Syntax

- `*`
- `random()`
- `random(seed = <seed>)`

The first two examples are equivalent.

> Note: Random block states can be waterloggable. If you hope they not be waterloggable, you can use `*[waterlogged=false]` or `dry(*)`.

Blocks have feature tag restrictions. Blocks not enabled (such as cherry-related blocks in 1.19.4 when experimental features are not enabled) will not be selected.

## Parameters

### `<seed>`

Long integer. The seed used to determine the random block states.