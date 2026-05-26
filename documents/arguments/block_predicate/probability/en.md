# `probability()`: Define Probability By Yourself

The [block predicate](../en.md) passes only in a specified probability. You can specify another block predicate, which passes only under the specific probability as well as that block predicate passes.

## Syntax

- `probability(<probability>, [block predicate])`
- `probability(<probability>, [block predicate], seed = <seed>)`

## Parameters

### `<probability>`

Floating-point number, between 0 and 1.

### `[block predicate]`

The [block predicate](../en.md) that should be fulfilled at the same time. If the block predicate is defined, it is equivalent to `probability(<概率>)*<方块谓词>`. If not defined, equivalent to `probability(<probability>, *)`.

### `<seed>`

Long integer.

## Examples

- `probability(0.35)`: Passes under the probability of 0.35.
- `probability(0.3, seed = 3)`: Passes under the probability of 0.3 with seed 3.
- `probability(0.8, stone)`: Passes under the probability of 0.8 and the block is stone.
- `probability(0.7, stone, seed = 5)`: Passes under the probability of 0.7 and the block is stone, with seed 5.

This block predicate can make a randomized effect in practice. For example, the command `/replaceblocks sphere(20) probability(0.4, dirt) bedrock` can replace about 40% dirt blocks within 20 blocks with bedrock.