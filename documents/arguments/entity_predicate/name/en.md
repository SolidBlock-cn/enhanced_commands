# `name`: Tests entity's name

This [entity predicate](../en.md) is same as vanilla `name` argument, testing entity's name and supporting inversion.

## Examples

- `@a[name=Steve]`
- `@e[name=!Alex]`

## Data structure

- `type`: Currently `"enhanced_commands:name"`.
- `name`: String/
- `inverted`: Boolean, optional, by default `false`.