# `health`: Tests entity's health value

This [entity predicate](../en.md) tests the health value of entities. Accepts a range, or the keyword `max`. The parameter can be inverted. This parameter only selects health values of living entities. Entities such as minecarts and dropped items also have their special health values but can’t be used to select by the entity selector.

## Examples

- `@e[health=20]`: Selects living entities whose health value is 20.
- `@e[health=!20]`: Selects living entities whose health value is not 20.
- `@e[health=max]`: Selects living entities whose health value is the max value.
- `@e[health=10..]`: Selects living entities whose health value is not less than 10.

## See also

- [the `/health` command](/documents/commands/health/en.md)

## Data structure

If the number value is a range:

- `type`: Currently `"enhanced_commands:health"`.
- `health`: Floating-point number range.
- `inverted`: Boolean, optional, by default `false`.

If the number value is specified to "max":

- `type`: Currently `"enhanced_commands:health_max"`.
- `inverted`: Boolean, optional, by default `false`.