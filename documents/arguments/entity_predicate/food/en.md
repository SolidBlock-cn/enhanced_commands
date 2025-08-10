# `food`: Tests the entity's food value

This [entity predicate](../en.md) tests the food value of entities. Accepts integer ranges. Can be inverted.

Only players have food values, so adding this parameter will only select players.

## Examples

- `@e[food=20]`: Select players whose food value is 20 (although `@e` is used, only players can be selected).
- `@a[food=10..15]`: Select players whose food value is between 10 and 15.
- `@a[food=!10]`: Select players whose food value is not 10.

## Data structure

- `type`: Currently `"enhanced_commands:food"`.
- `food`: Integer range.
- `inverted`: Boolean, optional, by default `false`。