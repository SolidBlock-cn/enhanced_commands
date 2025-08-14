# `/testfor blocks`

此[命令](../zh.md)用于对多个方块进行测试，并尝试找到符合（或不符合）指定谓词的方块，并提供该方块的具体信息，并提供查询方块信息、单独测试方块和传送到方块的快捷按钮。此命令也可以统计符合谓词的方块数量和比例。

## 语法

- `/testfor blocks <区域> <方块谓词> [测试类型] [关键字参数]`

## 参数

### `<区域>`

需要检测方块的[区域](/documents/arguments/region/zh.md)。

### `<方块谓词>`

需要检测方块匹配的[方块谓词](/documents/arguments/block_predicate/zh.md)。

### `[测试类型]`

可以支持以下值：

- `any`：如果区域内存在符合该谓词的方块，则测试成功，并返回该方块的信息。如果全都不符合，则测试失败。
- `all`：如果区域内的方块全都符合谓词，则测试成功，如果有不符合的，则测试失败，并返回该方块的信息。
- `compare`：统计区域内符合与不符合该谓词的方块的数量，并进行对比。如果符合谓词的方块不小于不符合谓词的方块，则测试成功。
- `count`：统计区域内符合与不符合该谓词的方块的数量。
- `proportion`：统计区域内符合谓词的方块的数量的占比。

### 关键字参数

- `immediately`：布尔值，默认为 `false`。参见 [`/setblocks`](../../setblocks/zh.md) 中的相关参数，下同。
- `bypass_limit`：布尔值，默认为 `false`。
- `unloaded_pos`：枚举，默认为 `reject`。
- `seed`：长整型。

## 示例

- `/testfor blocks sphere(5) dirt`
- `/testfor blocks outwards(10 3) short_grass|tall_grass all`
- `/testfor blocks sphere(20) air proportion`
- `/testfor blocks sphere(15) water|[waterlogged=true] compare`
- `/testfor blocks outwards(25) idcontain(ore) count immediately=true`