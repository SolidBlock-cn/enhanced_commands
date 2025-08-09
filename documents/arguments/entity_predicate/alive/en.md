# `alive`

This [entity predicate](../en.md) tests whether the entity is alive. Currently, specifying via entity selector arguments is not supported, but you can specify with different entity selector types.

## Examples

- `@e`: All alive entities.
- `@a`: All players, including dying ones.
- `@e[type=player]`: All alive players.
- `@E`: All entities, including dying ones.
- `@a[not=@e]`: Dying players.
- `@E[not=@e]`: Dying entities.

## Data structure

No extra fields.

- `type`: Currently `"enhanced_commands:alive"`.