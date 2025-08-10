# `local_world`: Special predicate to filter local world entities

This [entity predicate](../en.md) passes only when the entity is in the same world as the command executor. This is a special predicate and cannot be specified via entity selectors. Entity selectors with arguments such as `distance`,`dx` will bring with this predicate.

## Data structure

- `type`: Currently `"enhanced_commands:local_world`.