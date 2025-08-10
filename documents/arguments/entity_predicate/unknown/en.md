# `unknown`: Unknown entity predicate

This [entity predicate](../en.md) represents predicates added during the entity selector parsing that are not recognized by this mod. It may be introduced by other mods.

## Data structure

- `type`: Currently `"enhanced_commands:unknown"`.

As not identified by this mod, full serialization is not supported, and it will be decoded be a predicate that always passes.