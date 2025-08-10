# `pose`: Tests entity's pose

This [entity predicate](../en.md) selects entities in the specified pose, and supports inversion.

Supported pose names include:

- `standing`
- `fall_flying`
- `sleeping`
- `swimming`
- `spin_attack`
- `crouching`
- `long_jumping`
- `dying`
- `croaking`
- `using_tongue`
- `sitting`
- `roaring`
- `sniffing`
- `emerging`
- `digging`

## Examples

- `@e[pose=crouching]`: Selects all crouching entities.
- `@a[pose=!swimming]`: Selects all entities not swimming.

## Data structure

- `type`: Currently `"enhanced_commands:pose"`.
- `pose`: String.
- `inverted`: Boolean, optional, by default `false`.