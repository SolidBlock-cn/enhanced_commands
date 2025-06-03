# NBT target: specifying whose NBT to modify

**NBT target** is an [argument type](../en.md). specifying where to store the NBT data. Note that NBT target is a whole NBT and usually is only a compound. When running the command actually, the path argument is usually needed too, specifying storing the data in the path of the data.

## Syntax

- `block <region>`: The NBT data of all block entities from a specified region (which can be a single position). If none of the block in the region is a block entity, it will fail.
- `entity <entity selector>`: The NBT data of specified entities. The selector can specify multiple entities.
- `literal <NBT function>`: Directly specifying NBT data, and modify the data. It usually does not affect the world actually, and is just used to test the modification result.
- `storage <id>`: The NBT data from a specified storage. Same as vanilla `/data ... storage <id>`.

For the usage of parameters, see [region](/documents/arguments/region/en.md), [entity selector](/documents/arguments/entity_selector/en.md) and [NBT function](../nbt_function/en.md).

## Examples

- `block 5 1 4`
- `block sphere(5)`
- `entity @e[type=cow,limit=1]`
- `entity @p[c=-5]`
- `storage namespace:id`

## See also

- [NBT source](../nbt_source/en.md)
- [`/nbt` command](/documents/commands/nbt/en.md)