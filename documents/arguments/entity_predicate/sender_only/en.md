# `sender_only`: Selects the command executor only

This [entity predicate](../en.md) passes only when the entity is the command executor. This predicate is technical and cannot be specified through entity selector arguments, but comes from entity selector `@s`.

When the command executor does not exist (such as executed by a command block or the sever), this predicate always does not pass.

## Data structure

- `type`: Currently `"enhanced_commands:sender_only"`.

No other fields.