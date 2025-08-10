# `sub_predicate`: In entity selectors filter entity that `is` or is `not` what you want

Specifies an [entity predicate](../en.md) and only passes when the entity also matches or does not match the predicate. This means filtering or excluding entities selected.

## Syntax

- `[is=<entity predicate>]`: Selects entities that match the specified predicate.
- `[not=<entity predicate>]`: Selects entities that do not match the specified predicate.

The argument can be inverted. `[is=!<entity predicate>]` is identical to `[not=<entity predicate>]`, and `[not=!<entity predicate>]` is identical to `[is=<entity predicate>]`.

## Examples

- `@p[is=@s]`: The nearest player, and meanwhile the entity should be the executor of the command.
- `@a[is=@e[type=cow]]`: All players that are cows. This entity selector is valid but obviously cannot select any entity.
- `@e[not=@s]`: All alive entities except the command executor.
- `@e[is=!@s]`: All alive entities except the command executor. Identical to `@e[not=@s]` in effect.
- `@e[not=[type=cow]]`: All alive entities except cows. Identical to `@e[type=!cow]` in effect.
- `@e[not=@s, not=@pets]`: All alive entities except the command executor and pets of the command executor. Identical to `@e[alternatives=![@s, @pets]]` in effect.
- `@e[type=cow, not=!@s]`: All alive cows that are the command executor. Identical to `@e[type=cow, is=@s]`, `@s[type=cow]`.

## Data structure

- `type`: Currently `"enhanced_commands:sub_predicate"`.
- `predicate`: Entity predicate.
- `inverted`: Boolean, optional, by default `false`.