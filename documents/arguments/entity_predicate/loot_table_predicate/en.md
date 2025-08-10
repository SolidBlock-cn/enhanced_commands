# `loot_table_predicate`: Tests entities through loot table predicates

This [entity predicate](../en.md) tests entities using loot table predicates. Different from vanilla `predicate` argument, this mod's `predicate` argument supports directly specifying loot table predicate JSON.

## Data structure

- `type`: Currently `"enhanced_commands:loot_table_predicate"`.
- `predicate`: String or map.
- `inverted`: Boolean, optional, by default `false`.