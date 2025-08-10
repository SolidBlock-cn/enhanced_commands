# `advancements`: Tests player's advancement

This [entity predicate](../en.md) tests player's advancement. Same as vanilla, but when specifying advancement criteria, quoted strings are allowed.

## Syntax

- `[advancements={<advancement id> = <boolean>, ...}]`
- `[advancements={<advancement id> = {<criterion> = <boolean>, ...}, ...}]`

## 数据结构

- `type`: Currently `"enhanced_commands:advancements`.
- `advancements`: Map.
    - `<进度 id>`: Boolean or map. If map, has the following fields:
        - `<准则>`: Boolean.