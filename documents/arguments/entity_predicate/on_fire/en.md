# `on_fire`: Tests whether the entity is on fire

This [entity predicate](../en.md) tests whether the entity is on fire. The value of the argument, and supports inversion, for example, `[on_fire=!true]` is identical to `[on_fire=false]`.

## Examples

- `@a[on_fire=false]`: All players not on fire.
- `@e[on_fire=true]`: All alive entities on fire.

## Data structure

- `type`: Currently `"enhanced_commands:on_fire"`.
- `expected`: Boolean.