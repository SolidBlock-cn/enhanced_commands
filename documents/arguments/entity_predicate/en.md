# Entity predicate: filtering entities with a syntax basically same to selectors

An **entity predicate** is an [argument type](../en.md) playing the role of a condition to test whether the entity matches. In this mod, entity predicates are implemented with [entity selectors](../entity_selector/en.md), and use the syntax identical to entity selectors.

When `limit` option is not provided in the entity selector used by the entity predicate, it directly tests the entity. If the `limit` option exists, it selects entities at first, and then tests whether the entity belongs to the selected entities. Generally speaking, as for entity predicates, the option `limit` is not recommended.

Besides, compared to entity selectors, entity predicates can ignore the “at-variable" before it. For example, `[type=cow]` is identical to `@E[type=cow]` (the difference between `@E` and `@e` is, `@E` can select dying entities, while `@e` only selects alive entities).

See [entity selectors](../entity_selector/en.md) for more information.

## Syntax

The syntax of entity predicates is the same as entity selectors, but can emit "at-variables".

- `<player id>`
- `<UUID>`
- `[<option 1>=<value 1>, <option 2>=<value 2>, ...]`: unique syntax of entity predicates compared to selectors.
- `<实体选择器类型>[<option 1>=<value 1>, <option 2>=<value 2>, ...]`

For entity selector types and arguments supported, see [entity selector](../entity_selector/en.md).

## Examples

- `Steve`: Passes if the entity is player Steve.
- `@e`: Passes when the entity is alive.
- `@vehicle`: Passes when the entity is the entity that the command executor is riding.
- `[baby=true]`: Passes when the entity is baby.
- `[type=cow]`: Passes when the entity is cow.

## Data structure

- `type`: String, the entity predicate type. For the full list of types see below.

Each entity predicate has a type. Different types have their own fields (see the page of corresponding types). The following is the ids of all entity predicate types (all namespace `enhanced_commands`, and the namespace is emitted in the list):

- [`advancements`](advancements/en.md)
- [`air`](air/en.md)
- [`air_max`](air/en.md)
- [`alive`](alive/en.md) (special type)
- [`alternatives`](alternatives/en.md)
- [`baby`](baby/en.md)
- [`block_predicate`](block/en.md)
- [`block_predicates`](block/en.md)
- [`box`](box/en.md)
- [`collector`](collector/en.md) (special type)
- [`distance`](distance/en.md) (special type)
- [`effect`](effect/en.md)
- [`effects`](effect/en.md)
- [`empty`](empty/en.md)
- [`exhaustion`](exhaustion/en.md)
- [`fire`](fire/en.md)
- [`food`](food/en.md)
- [`game_mode`](game_mode/en.md)
- [`health`](health/en.md)
- [`health_max`](health/en.md)
- [`level`](level/en.md)
- [`local_world`](local_world/en.md) (special type)
- [`loot_table_predicate`](loot_table_predicate/en.md)
- [`name`](name/en.md)
- [`nbt`](nbt/en.md)
- [`owner`](owner/en.md)
- [`pitch`](pitch/en.md)
- [`player_name`](player_name/en.md)
- [`player_only`](player_only/en.md)
- [`pose`](pose/en.md)
- [`on_fire`](on_fire/en.md)
- [`region`](region/en.md)
- [`saturation`](saturation/en.md)
- [`sender_only`](sender_only/en.md) (special type)
- [`selector`](selector/en.md) (special type)
- [`sneaking`](sneaking/en.md)
- [`sprinting`](sprinting/en.md)
- [`sub_predicate`](sub_predicate/en.md)
- [`swimming`](swimming/en.md)
- [`tag`](tag/en.md)
- [`team`](team/en.md)
- [`type`](type/en.md)
- [`types`](type/en.md)
- [`type_tag`](type/en.md)
- [`unknown`](unknown/en.md)
- [`uuid`](uuid/en.md)
- [`yaw`](yaw/en.md)

特殊类型是指无法通过实体选择器参数直接指定，而是由原版的实体选择器参数间接指定或者由实体选择器类型指定的。