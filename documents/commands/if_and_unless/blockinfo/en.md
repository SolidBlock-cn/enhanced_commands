# `/if|unless blockinfo`

The [command](../../en.md) is used to execute the command only when the block info match or does not match the specified condition.

## Syntax

- `/if|unless blockinfo <pos> hardness <float range> ...`
- `/if|unless blockinfo <pos> luminance <int range> ...`
- `/if|unless blockinfo <pos> strong_redstone_power <direction> <int range> ...`
- `/if|unless blockinfo <pos> weak_redstone_power <direction> <int range> ...`
- `/if|unless blockinfo <pos> light <int range> ...`
- `/if|unless blockinfo <pos> block_light <int range> ...`
- `/if|unless blockinfo <pos> sky_light <int range> ...`
- `/if|unless blockinfo <pos> emits_redstone_power ...`
- `/if|unless blockinfo <pos> opaque ...`
- `/if|unless blockinfo <pos> model_offset ...`
- `/if|unless blockinfo <pos> suffocate ...`
- `/if|unless blockinfo <pos> block_vision ...`
- `/if|unless blockinfo <pos> replaceable ...`
- `/if|unless blockinfo <pos> random_ticks ...`

After completing the commands above, a complete command can be added after.

The meaning of the commands above see [`/testfor blockinfo`](/documents/commands/testfor/blockinfo/en.md).

## Parameters

### `<pos>`

[Pos](/documents/arguments/pos/en.md). The coordinate of the block to be tested.

## Examples

- `/if blockinfo ~ ~ ~ light 6.. tellraw @a "There is light here"`
- `/if blockinfo ~ ~ ~ replaceable setblock ~ ~ ~ stone`
- `/unless blockinfo ~ ~-1 ~ luminance 10.. setblock ~ ~-1 ~ glowstone`

## See also

- [`/testfor blockinfo` command](/documents/commands/testfor/blockinfo/en.md)