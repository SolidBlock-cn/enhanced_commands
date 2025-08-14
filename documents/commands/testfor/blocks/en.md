# `/testfor blocks`

This [command](../en.md) is used to test on multiple blocks, and try to find the block that matches (or does not match) the specified predicate, provide the detailed information of the block, and provide utility buttons to query the block information, test the single block and transport to the block. This command can also statistic the count and proportion of blocks that match the predicate.

## Syntax

- `/testfor blocks <region> <block predicate> [test type] [keyword args]`

## Parameters

### `<region>`

The [region](/documents/arguments/region/en.md) where block to be tested/

### `<block predicate>`

The [block predicate](/documents/arguments/block_predicate/en.md) to test the block matches.

### `[test type]`

Supports the following values:

- `any`: If there is a block that matches this predicate, test succeeds, and returns the information of this block. If none matches, the test fails.
- `all`: If all blocks in the region match the predicate, test succeeds. If there is a block that does not match, the test fails, and returns the information of this block.
- `compare`: Statistic the count of blocks that match and do not match this predicate in the region, and compare. If the number of blocks that match the predicate is not lower than the number of blocks that do not match the predicate, the test succeeds.
- `count`: Statistic the count of blocks that match and do not match this predicate in the region.
- `proportion`: Statistic the proportion of blocks that match the predicate in the region.

### Keyword arguments

- `immediately`: Boolean, by default `false`. See relevant arguments in [`/setblocks`](../../setblocks/en.md), vice versa.
- `bypass_limit`: Boolean, by default `false`.
- `unloaded_pos`: Enum, by default `reject`.
- `seed`: Long integer.

## Examples

- `/testfor blocks sphere(5) dirt`
- `/testfor blocks outwards(10 3) short_grass|tall_grass all`
- `/testfor blocks sphere(20) air proportion`
- `/testfor blocks sphere(15) water|[waterlogged=true] compare`
- `/testfor blocks outwards(25) idcontain(ore) count immediately=true`