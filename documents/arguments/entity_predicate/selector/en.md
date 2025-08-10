# `selector`: Predicate based on entity selector

This [entity predicate](../en.md) tests whether the entity matches the specified entity selector. This is the primary implementation of entity predicates in this mod.

When the entity selector has `limit` argument, when used as an entity predicate, it selects entities first, and then judge whether the entity belongs to the selected entities. If `limit` argument is not specified, the entity predicates specified by the selector will be directly used to judge.

## Data structure

- `type`: Currently `"enhanced_commands:selector"`.
- Other fields are identical to fields of [entity selectors](../../entity_selector/en.md).