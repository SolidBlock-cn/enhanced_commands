# `region`: Selects entities in the specified region

This [entity predicate](../en.md) selects entities in the specified [region](/documents/arguments/region/en.md). Relative coordinates are local coordinates are supported, which are calculated according to the position and rotation of the command source (instead of the selected entities' position and rotation).

This entity predicate does not support inversion yet.

## Examples

- `@e[region=sphere(5)]`: Selects entities within a sphere range, whose center is the command source's position, and radius is 5.

## Data structure

- `type`: Currently `"enhanced_commands:region"`.
- `region`: [Region](/documents/arguments/region/en.md).