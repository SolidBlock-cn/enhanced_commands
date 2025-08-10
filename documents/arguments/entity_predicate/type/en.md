# `type`: Tests the entity type

This [entity predicate](../en.md) tests the entity type. You can specify one type, or multiple types. The specified type can be the entity type id, or the entity type tag. This argument also supports inversion.

Usually, for entity selectors, if the `type` argument only specify one type, is not tag and is not inverted, it will be used as a property of the entity predicate itself, and may affect the logic of entity selector selecting entities.

## Syntax

- `[type=<type>]`: Selects entities of the specified type. The `<type>` here can be an entity id (such as `cat`), or a tag (such as `#skeletons`). Same below.
- `[type=!<type>]`: Selects entities not of the specified type.
- `[type=<type 1>|<type 2> ...]`: Selects entities of any one of the specified multiple types. The types here cannot be duplicate. Same below.
- `[type=!<type 1>|<type 2> ...]`: Selects entities of none of the specified multiple types.

## Examples

- `@p[type=cat|wolf]`: The nearest cat or wolf.
- `@e[type=player|iron_golem]`: All alive players or iron golems.
- `@e[type=zombie|zombie]`: Invalid, due to duplicate values.

## Data structure

If only one type is specified, and it is not tag:

- `type`: Currently `"enhanced_commands:type"`.
- `entity_type`: String.
- `inverted`: Boolean, optional, by default `false`.

若只指定一个类型，且是标签：

- `type`: Currently `enhanced_commands:type_tag"`.
- `tag`: String, tag id, including hash symbol.
- `inverted`: Boolean, optional, by default `false`.

若指定了多个类型：

- `type`: Currently `enhanced_commands:types`.
- `types`: List.
    - Element of the list. String. Entity id or tag id (including hash symbol).
- `inverted`: Boolean, optional, by default `false`.