# `level`

This [entity predicate](../en.md) is used to test the player's experience level. Usage is basically the same as vanilla, but difference is this mod supports inverting it.

Entity selectors using this predicate only select player.

## Examples

- `@a[level=10..]`: Selects players whose experience level is at least 10.
- `@a[level=!5]`: Selects players whose experience level is not 5.

## Data structure

- `type`: Currently `"enhanced_commands:level"`.
- `level`: Integer range.
- `inverted`: Boolean value, optional, by default `false`.