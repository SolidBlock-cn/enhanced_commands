## `baby`: Selects baby entities

This [entity predicate](../en.md) tests whether the entity is a baby. It takes a boolean value. If the entity type doesn’t support baby entities, entities will be seen as non-babies.

## Examples

- `@e[baby=true]`: Selects all baby entities.
- `@e[baby=false]`: Selects all entities who aren’t babies (including entities who can’t be babies).

The parameter can be inverted. For example, `@e[baby=!true]` is equivalent to `@e[baby=false]`.

## Data structure

- `type`: Currently `"enhanced_commands:baby"`.
- `expected`: Boolean.