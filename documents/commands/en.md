This mod adds a series of **commands**.

## Region commands

- [`/activeregion`](activeregion/en.md) or `/ar` and `//activeregion` and `//ar`: Manage the player's active region.
- [`/regionselection`](regionselection/en.md) or `/rs`: Manage the player's region selection type.

## Block commands

These commands are used to operate on blocks (sometimes including entities) within a range.

- [`/convertblock`](convertblock/en.md): Convert the block to a specific entity (such as display entity, falling block).
- [`/convertblocks`](convertblocks/en.md) and `//convertblocks`: Convert the blocks within a range to specific entities.
- [`/draw`](draw/en.md): Draw a curve in the world.
- [`/mirror`](mirror/en.md) and `//mirror`: Mirror (flip) the blocks and entities within a range.
- [`/move`](move/en.md) and `//move`: Move the blocks and entities within a range.
- [`/outline`](outline/en.md): Fill the blocks on the outline of a region. It can also fill the inner part within the outline meanwhile.
- [`/postprocess`](postprocess/en.md) and `//postprocess`: Postprocess on blocks, such as fixing connection issues related to fences and walls.
- [`/replace`](replace/en.md) and `//replace`: Replace blocks within a region that match the specified predicate.
- [`/rotate`](rotate/en.md) and `//rotate`: Rotate blocks and entities within a region.
- [`/setblocks`](setblocks/en.md), `//setblocks` and `/s`, `//s`: Set the blocks within a region.
- [`/stack`](stack/en.md) and `//stack`: Duplicate the region in one direction.
- [`/wall`](wall/en.md): Fill the walls in a region. It can also fill the inner part within the wall meanwhile.

## Entity commands

- [`/air`](air/en.md): Modify the entity's air value.
- [`/food`](food/en.md): Modify the entity's food value, saturation and exhaustion.
- [`/health`](health/en.md): Modify the entity's health value.
- [`/pile`](pile/en.md): Pile entities by riding.
- [`/tame`](tame/en.md): Tame or cancel taming the entity.
- [`/tprel`](tprel/en.md) Teleporting entities, and the coordinates and rotations are calculated based on each entity to be transported.

## Test commands

- [`/testarg`](testarg/en.md): Test on argument types.
- [`/testfor`](testfor/en.md): Test and get data in the current world.

## Independent `/execute` subcommands

The subcommands of `/execute` are separated to single commands (former grammar is still normally available). For example, `/execute as @e at @s run summon creeper` can be written as `/as @e at @s summon creeper`.

Separated subcommands of `/execute` include:

- Condition subcommands: `/if` and `/unless`
    - Vanilla: `block`, `biome`, `loaded`, `dimension`, `score`, `blocks`, `entity`, `predicate`, `function`, `items`, `data` etc.
    - [`blockinfo`](if_and_unless/blockinfo/en.md): Execute the command when the block info match or does not match the specified condition.
    - [`rand`](if_and_unless/rand/en.md): Execute or do not execute the command on a specified probability.
- Attribution subcommands:
    - `/as`, `/at`, `/positioned`, `/rotated`, `/facing`, `/align`, `/anchored`, `/in`, `/summon` etc.
    - [`/inregion`](inregion/en.md) (added in the mod): Execute commands in each block coordinates within a range.
    - [`/silenced`](silenced/en.md) (added in the mod): Execute commands without leaving any feedback in the chat or console.
    - [`/for`](for/en.md) (added in the mod)
- Storage subcommands:
    - `/store`

## Simplified `/gamemode` commands

Commands of several game modes are all simplified.

- `/gmc [players]` is equivalent to `/gamemode creative [players]`
- `/gms [players]` is equivalent to `/gamemode survival [players]`
- `/gma [players]` is equivalent to `/gamemode adventure [players]`
- `/gmsp [players]` is equivalent to `/gamemode spectator [players]`

## Debug commands

- [`debug:deop`](debug_deop/en.md): Force to repeal admission permission from players, even if the executor does not have permissions.
- [`debug:op`](debug_op/en.md): Force to give players admission permission, even if the executor does not have permissions.
- [`debug:permissionlevel`](debug_permissionlevel/en.md): View the current permission level, or simulate the specified permission level to execute commands.

## Other commands:

- [`/history`](history/en.md): Manage operation history.
- [`/moon`](moon/en.md): Query and set moon phase. `/jadeplate` is an alise to this command.
- [`/nbt`](nbt/en.md): Query and set NBT data.
- [`/rand`](rand/en.md): Generate random number.
- [`/tasks`](tasks/en.md): Manage tasks in the current server.
- [`/undo` 和 `/redo`](undo_and_redo/en.md): Undo and redo.
