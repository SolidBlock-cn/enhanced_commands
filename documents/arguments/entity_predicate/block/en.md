# `block`: Tests the block where the entity is

The [entity predicate](../en.md) can be used to test any coordinate based on the position where the entity is, including relative coordinates and local coordinates. You can specify a single [block predicate](/documents/arguments/block_predicate/en.md), or specify multiple pairs composed of a [coordinate](/documents/arguments/pos/en.md) and a [block predicate](/documents/arguments/block_predicate/en.md) to test blocks at the specified coordinates.

## Syntax

- `[block=<block predicate>]`: Specify a single block predicate to test the block where the entity is.
- `[block={<coordinate 1> = <block predicate 1>, <coordinate 2> = <block predicate 2>, ...}]`: Specify multiple coordinates and block predicates to test the blocks at the specified positions.

> Note: If the block predicate is an [NBT predicate](/documents/arguments/block_predicate/nbt/en.md), please add parenthesis or star to the NBT predicate or be parsed as the second syntax. For example, `[block={Inventory: []}]` is incorrect, and should be written as `[block=({Inventory: []})]` or `[block=*{Inventory: []}]`.

## Examples

- `@e[block=air]`: Selects entities where the block is air.
- `@e[block=water|*[waterlogged=true]]`: Selects entities where the block is water or any waterlogged block.
- `@a[block={~~-1~=redstone_block}]`: Selects entities where the block at one block below is a redstone block.
- `@e[block={~~-1~=grass_block, ~~-2~=dirt}]`: Selects entities where the block at one block below is grass block and two blocks below is dirt.

## Data structure

When directly specifying a single block predicate:

- `type`: Currently `"enhanced_commands:block_predicate"`.
- `predicate`: [Block predicate](/documents/arguments/block_predicate/en.md).

When specifying coordinates and block predicates:

- `type`: Currently `"enhanced_commands:block_predicates"`.
- `predicates`: List.
    - Element in the list.
        - `pos`: [Coordinate](/documents/arguments/pos/en.md).
        - `block`：[Block predicate](/documents/arguments/block_predicate/en.md).