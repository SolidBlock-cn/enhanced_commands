# `distance`

This [entity predicate](../en.md) exists technically, and is used to test the entity's distance to the position defined by the `x`, `y`, `z` arguments.

In the entity selector arguments, the range of distance is specified through `distance` argument, and the base point of the distance is specified through `x`, `y`, `z` arguments.

## Data structure

- `type`: Currently `"enhanced_commands:distance"`.
- `distance`: Double floating-point number or map.
    - `min`: Double floating-point number.
    - `max`: Double floating-point number.
- `info`: Map. Representing a position offset specified by `x`, `y`, `z` arguments.