# Entity predicate: filtering entities with a syntax basically same to selectors

An **entity predicate** is an [argument type](../en.md) playing the role of a condition to test whether the entity matches. In this mod, entity predicates are implemented with [entity selectors](../entity_selector/en.md), and use the syntax identical to entity selectors. However, different from entity selectors, when `limit` option is not provided in the entity selector used by the entity predicate, it runs directly on the entity. If the `limit` option exists, entities will be selected at first, and then test whether the entity belongs to the selected entities. Generally speaking, as for entity predicates, the option `limit` is not recommended.

Besides, compared to entity selectors, entity predicates can ignore the “at-variable" before it. For example, `[type=cow]` is identical to `@E[type=cow]` (the difference between `@E` and `@e` is, `@E` can select dying entities, while `@e` only selects alive entities).

See [entity selectors](../entity_selector/en.md) for more information.