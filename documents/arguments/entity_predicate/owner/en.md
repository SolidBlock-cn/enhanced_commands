# `owner`: Tests the owner of tameable entities

This [entity predicate](../en.md) filters by entities' owners. The value can be an [entity predicate](../en.md) or no value.

This option can be inverted. When no value is specified, it selects untamed entities, including untamable entities.

- `@e[owner=]`: Selects all entities not tamed, including untamable entities.
- `@e[owner=!]`: Selects all tamed entities, regardless of the owner (can be players no in the current world).

If a selector is provided as a value, then no matter inverted or not, entities can’t be selected when they’re untamable, the owner doesn’t exist, or not in this world.

- `@e[owner=<实体谓词>]`: Selects all tamed entities whose owners match the specified entity predicate.
- `@e[owner=!<实体谓词>]`: Selects all tamed entities whose owners don’t match the specified entity predicate.

## Examples

- `@e[owner=Alice]`: Selects all entities tamed by player Alice. It fails when Alice is not in the world.
- `@e[owner=!@p]`: Selects all entities tamed by any player who is not the nearest player to the execution position.
- `@e[owner=@s]`: Selects all entities tamed by the executor of the command, similar to `@pets`.

## Data structure

- `type`: Currently `"enhanced_commands:owner"`.
- `owner`: [Entity predicate](../en.md).
- `inverted`: Boolean, optional, by default `false`.