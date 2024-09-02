# `ifs()`

此[方块函数](../zh.md)相当于多个 [`if()` 方块函数](../if/zh.md) 叠加的一种简写的形式。

## 语法

- `ifs(<方块谓词 1>, <方块函数 1a>, [方块函数 1b]; <方块谓词 2>, <方块函数 2a>, [方块函数 2b]; <方块谓词 3>, <方块函数 3a>, [方块函数 3b]; ...)`

该函数的参数由多个组组成，各组由分号“`;`”隔开（最后一组后面不能有分号）。每一组由 1 个方块谓词和 1 到 2 个方块函数组成，由逗号“`,`”隔开（最后一项后面不能有逗号），相当于 `if()` 函数内的内容。

在运行时，会依次匹配各组的方块谓词，如果满足该组的方块谓词，则使用该组的方块函数 a。如果每组的方块谓词均不符合，则使用最后一组的方块函数 b。如果没有组，或者最后一组没有方块函数 b，则不执行操作。

实际上相当于 `if()` 方块函数的嵌套，例如：

- `ifs(P1, F1a, F1b)` 相当于 `if(P1, F1a, F1b)`
- `ifs(P1, F1a; P2, F2a, F2b)` 相当于 `if(P1, F1a, if(P2, F2a, F2b))`
- `ifs(P1, F1a; P2, F2a; P3, F3a, F3b)` 相当于 `if(P1, F1a, if(P2, F2a, if(P3, F3a, F3b)))`

> 请注意，只有所有方块谓词都不匹配时，才使用最后一组的方块函数 b。非最后一组的方块函数 b，在任何情况下都不会使用，所以虽然能正常解析，但没有必要指定，例如：
>
> - <code>ifs(P1, F1a, ~~F1b~~; P2, F2a, ~~F2b~~; P3, F3a, F3b)</code>
> - `ifs(P1, F1a; P2, F2a; P3, F3a)`

## 示例

- `ifs(oak_log, oak_planks; birch_log, birch_planks)`：当方块是橡木原木时，替换为橡木木板，当方块是白桦原木时，替换为白桦木板。其他情况下不操作。
- `ifs(oak_log, oak_planks; birch_log, birch_planks; #logs, stone)`：当方块是橡木原木时，替换为橡木木板，当方块是白桦原木时，替换为白桦木板；当方块是其他原木时，替换为石头。其他情况下不操作。
- `ifs(oak_log, oak_planks; birch_log, birch_planks; #logs, stone, air)`：当方块是橡木原木时，替换为橡木木板，当方块是白桦原木时，替换为白桦木板；当方块是其他原木时，替换为石头。其他情况则替换为空气。
- `ifs(#logs, stone; oak_log, oak_planks; birch_log, birch_planks)`：当方块是原木时，替换为石头。其他情况不指定。如果方块是橡木原木、白桦原木时，第一组条件的 `#logs` 已经满足，所以第二、三组条件将不会使用到。
- `ifs(#logs, stone; oak_log, oak_planks;)`：语法错误，无法解析，因为最后一组条件后面不能再有分号。
- `ifs(oak_log, oak_planks; birch_log, birch_planks; spruce_log, spruce_planks; cherry_log, cherry_planks)`：无需解释。

## 参见

- [`if()`](../if/zh.md)