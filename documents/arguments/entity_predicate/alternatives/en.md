# `alternatives`: union set of multiple entity predicates

This [entity predicate](../en.md) multiple entity predicates. The entity only needs to pass one of the predicate tests. Values of the parameters are wrapped in a square bracket, with multiple entity predicates inside; multiple entity predicates are separated with comma. The parameter be inverted.

## Examples

- `@e[alternatives=[[type=cow], [type=sheep]]]`: Selects all cows and sheep.
- `@a[alternatives=[Player1, Player2]]`: Selects Player1 and Player2.
- `@e[type=sheep, alternatives=[[tag=a], [tag=b]]]`: Within all sheep, select those with tag "a" or tag "b".
- `@e[type=sheep, alternatives=![[tag=a], [tag=b]]]`: Within all sheep, select those with neither tag "a" nor "b".

## Data structure

- `type`: Currently `"enhanced_commands:alternatives"`.
- `predicates`: List.
    - An [entity predicate](../en.md)。
- `inverted`: Boolean/