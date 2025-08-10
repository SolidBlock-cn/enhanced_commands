# `sneaking`: Tests sneaking

This [entity predicate](../en.md) tests whether the entity is sneaking. Players flying and descending will also be selected. This option is different from the loot predicate.

## Syntax

- `@e[sneaking=true]`: Selects sneaking entities.
- `@e[sneaking=false]`: Selects entities not sneaking.

The parameter can be inverted but usually unnecessary. For example, `@e[sneaking=!true]` is equivalent to `@e[sneaking=false]`.

## Data structure

- `type`: Currently `"enhanced_commands:sneaking"`.
- `expected`: Boolean.