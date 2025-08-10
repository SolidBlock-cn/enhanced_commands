# `saturation`: Tests entity's saturation

This [entity predicate](../en.md) tests entity's saturation. It accepts a floating-point number range, and supports inversion.Similar to [`food`](../food/en.md), only players can be selected when this parameter is added.

## Examples

- `@e[saturation=5..]`: Selects players whose saturation is at least 5.
- `@e[saturation=!0]`: Selects players whose saturation is not 0.

## Data structure

- `type`: Currently `"enhanced_commands:saturation"`.
- `saturation`: Floating-point number range.
- `inverted`: Boolean, optional, by default `false`.