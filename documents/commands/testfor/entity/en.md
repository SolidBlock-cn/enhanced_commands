# `/testfor entity`

This [command](../../en.md) is used to test on entities, and test entity predicates on the entities selected, tell whether they match the entity predicate. If there is only one entity, a detailed description will be thrown during the predicate test.

## Syntax

- `/testfor entity <entity selector`: Select entities with an [entity selector](/documents/arguments/entity_selector/en.md). If there are entities selected, the name and amount of the entities will be shown. If the number of entities is large, only some entities' name will be shown, and show the number of entities. The integer value returned by the command is the number of entities selected.
- `/testfor entity <entity selector> <entity predicate>`: Tell whether the entities match the [entity predicate](/documents/arguments/entity_predicate/en.md). If one entity is selected, the detailed description of the predicate test will be shown. If multiple entities are selected, the number of entities that match the predicate will be shown. The integer value returned by the command is the number of entities matching the predicate.

## Examples

- `/testfor entity @s`
- `/testfor entity @e[type=cat]`
- `/testfor entity @s [gamemode=creative, effect=!blindness]`
- `/testfor entity @e[c=5] [block=air]`