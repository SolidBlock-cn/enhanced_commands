# Block function

The **block function** is an [argument type](../en.md) used to modify a block (including block entity) at a certain position in the world.

## Basic usage

Block functions can use block ID and can also specify block state properties and NBT, like vanilla. Generally speaking, original blocks are directly ignored in these cases. For example:

- `minecraft:stone`: Stone.
- `minecraft:redstone_lamp[lit=true]`: Lit redstone lamp.
- `minecraft:repeating_command_block{Auto: true}`: Repeating command block that runs automatically.

## Compound block function

Multiple block functions can be compounded to produce a more complex block function.

- `<block function 1>*<block function 2>`: Two or more block functions are applied in order. The result of the first function acts as the parameter for the second function.
- `<block function 1>|<block function 2>`: Two or more block functions are selected randomly. One of the two block functions will be randomly chosen to apply.

The order of the two block functions above is, first `*` then `|`. For example, `a|b*c` is identical to `a|(b*c)`.

These two syntaxes cannot be space-separated, but if they are in a parentheses, space is allowed. For example, `a | b` is invalid, but `(a | b)` and `dry(a | b)` are valid.

For example (all the examples below omitted namespace):

- `stone|dirt|grass_block`: Randomly select stone, dirt and grass block.
- `black_wool|white|wool`: Randomly select black wool and white wool.

## Full list

### Non-function-like syntax

- [Simple block function: `block id`](simple/en.md): Simply use the block of the specified ID.
- [Tag block function: `#tag id`](tag/en.md): Randomly choose a block from the block tag.
- [Properties block function: `[proeprty=value]`](property_names/en.md): Modify the properties of the current block.
- [NBT block function: `{}`](nbt/en.md): Modify the block's NBT data.
- [Properties NBT combination: `block id[...]{...}`](property_nbt_combination/en.md): Set the id or tag, block state properties and NBT of blocks, which is similar to vanilla usage.
- [Random block function: `*`](random/en.md): Randomly choose a block and random select a block state.
- [Original block function: `~`](use_original/en.md): Use the block before the whole function evaluation.
- [Reference block function: `$`](reference/en.md): Reference a block function defined in the data pack.

### Function-like syntax

- [`checkerboard(<block function> [weight] ...)`](checkerboard/en.md): Checkerboard pattern.
- [`checkerboard-tag(<block function> [weight] ...)`](checkerboard-tag/en.md): Checkerboard pattern whose content is the blocks with a block tag.
- [`dry([block function])`](dry/en.md): Remove water from the current block, or apply the block function and then remove water.
- [`filter(<block function 1>, <block predicate>, [block function 2])`](filter/en.md): Calculate the result of block function 1 first, and if the result of block function 1 matchest the block predicate, then it is applied directly, or block function 2 will be calculated, or do nothing.
- [`idcontain(<regex>)`](idcontain/en.md): Randomly pick a block whose ID matches the specified regular expression.
- [`idreplace(<regex>)`](idreplace/en.md): Replace the block ID. If the replaced block ID still exists, it will be applied.
- [`if(<block predicate>, <block function 1>, [block function 2])`](if/en.md): Test which the former block match the block predicate, and if so, use block function 1, or use block function 2 to do nothing.
- [`ifs(<block predicate 1>, <block function 1a>, [block function 1b]; ...)`](ifs/en.md): Test multiple block predicates.
- [`mirror(<方向>)`](mirror/en.md): Mirror the current block.
- [`noise(<block function> [weight], ...; ...)`](noise/en.md): Noise.
- [`overlay([block function], ...)`](overlay/en.md): Apply multiple block functions in order.
- [`pick(<block function>, ...)`](pick/en.md): Randomly choose a block function.
- [`postprocess<direction> ...`](postprocess/en.md): Postprocess blocks, such as solving connection issues of fences and walls.
- [`random()`](random/en.md): Randomly chose a block state. Similar to the random block function above, but supports specifying the seed.
- [`rotate(<方向>)`](rotate/en.md): Rotate the current block.
- [`stonecut([block function])`](stonecut/en.md): Apply stonecutting on the current block or the result of the specified block function. If there are multiple stonecut results, select one stonecut result randomly.

## Data structure

- `type`: String. ID of the block function type.
- Fields that the type may have.

There are following types of block functions (the namespace `enhanced_commands` are all omitted in the list):

- [`simple`](simple/en.md)
- [`property_names`](property_names/en.md)
- [`nbt`](nbt/en.md)
- [`properties_nbt_combination`](property_nbt_combination/en.md)
- [`empty`](empty/en.md)
- [`random`](random/en.md)
- [`tag`](tag/en.md)
- [`use_original`](use_original/en.md)
- [`checkerboard`](checkerboard/en.md)
- [`checkerbard-tag`](checkerboard-tag/en.md)
- [`conditional`](if/en.md)
- [`conditions`](ifs/en.md)
- [`dry`](dry/en.md)
- [`filter`](filter/en.md)
- [`id_contain`](idcontain/en.md)
- [`id_replace`](idreplace/en.md)
- [`mirror`](mirror/en.md)
- [`noise`](noise/en.md)
- [`overlay`](overlay/en.md)
- [`pick`](pick/en.md)
- [`post_process`](postprocess/en.md)
- [`reference`](reference/en.md)
- [`rotate`](rotate/en.md)
- [`stonecut`](stonecut/en.md)