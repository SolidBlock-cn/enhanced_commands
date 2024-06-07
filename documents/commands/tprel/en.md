# `/tprel`

This [command](../en.md) is used to teleport entities. But different from vanilla `/teleport` (`/tp`), the relative coordinates and relative rotations of `/tprel` are based on the position and rotation of each entity to be teleported, instead of those of the command source (which is usually the position and rotation of the executor of the command). `/tprel` does not support teleporting entities to another entity.

## Syntax

- `/tprel <position>`: Teleport the current entity to the specified position.
- `/tprel <target> <position> [rotation]`: Teleport the target entities to the specified position, and set its rotation.
- `/tprel <target> <position> facing <facing position>`: Teleport the target entities to the specified position, and set their rotation to face a specified position.
- `/tprel <target> <position> facing entity <facing entity> [facing anchor]`: Teleport the target entities to the specified position, and set their rotation to face the specified anchor position of specified entity.

## Parameters

### `<position>`

The position to be teleported to.

### `<target>`

Entity selector. Entities to be teleported.

### `[rotation]`

Rotation degree.

### `<facing position>`

The position entities look at after being teleported.

### `<facing entity>`

Entity selector. Must be single entity.

### `[facing anchor]`

Value can be `feet` or `eyes`. By default `feet`.

## Examples

To distinguish `/tprel` of this mod from vanilla `/tp`, examples of `/tp` are also shown here.

- `/tp @e ~ ~ ~`: Teleport all entities to the position of command executor.
- `/tprel @e ~ ~ ~`: Teleport all entities to their own positions, equivalent to no-op.
- `/tp @e[type=chicken] ~ ~5 ~`: Teleport all chickens to the position 5 blocks above the command executor.
- `/tprel @e[type=chiecken] ~ ~5 ~`: Teleport all chickens to positions 5 blocks above themselves, which means, all move up 5 blocks.
- `/tp @e[distance=..5] ^ ^ ^3`: Teleport all entities within 5 blocks to the position 3 blocks front of the command executor in its looking direction.
- `/tprel @e[distance=..5] ^ ^ ^3`: Teleport all entities within 5 blocks to the positions 3 blocks of them in their own looking directions, which means, all move forward 5 blocks.