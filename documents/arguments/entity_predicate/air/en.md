## `air`: Tests the entity's air

The [entity predicate](../en.md) tests the air value of the entity. Accepts int ranges or the keyword `max` which means the max air value. Can be inverted.

## Examples

- `@e[air=5..]`: Selects entities whose air values aren’t less than 5.
- `@a[air=!..0]`: Selects entities whose air values are greater than 0.

## See also

- [the `/air` command](/documents/commands/air/en.md).

## Data structure

When the type is `air`:

* `type`: Currently `"enhanced_commands:air"`
* `air`: Int range.
* `inverted`: Boolean, optional, `false` by default.

When the type is `air_max`:

* `inverted`: Boolean, optional, `false` by default.
