# `postprocess()`: Settle your fences and walls

This [block function](../en.md) cannot postprocess on blocks, such as triggering a block update, which may address the issues related to fences and walls.

To demonstrate the function of this function, you can try executing the following commands in order (you can also add some extra arguments to adjust the execution effect):

```
/setblocks outwards(5 0) #walls[waterlogged=false, *] force=true
/setblocks outwards(5 0) postprocess()
```

## Syntax

- `postprocess()`: Postprocess in all directions.
- `postprocess(all)`: Postprocess in all directions.
- `postprocess(horizontal)`: Postprocess in horizontal directions.
- `postprocess(vertical)`: Postprocess in vertical directions.
- `postprocess(<direction> ...)`: Postprocess in the specified directions.

## Parameters

### `<direction> ...`

Direction. To specify multiple directions, separate them with a space.

## Examples

- `postprocess()`: Postprocess on all directions (settle connection issues in all directions).
- `postprocess(east)`: Postprocess on east directions (settle connection issues in the east direction).

## See also

- [the `/postprocess` command](/documents/commands/postprocess/en.md)

## Data structure

- `type`: Currently `"enhanced_commands:post_process"`.
- `directions`: List.
    - Element of the list. String.