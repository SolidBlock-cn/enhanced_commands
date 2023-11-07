# `/convertblocks` 和 `//convertblcks`

此[命令](../zh.md)用于将一个区域内的方块全部转换为实体。

## 语法

- `/convertblocks <区域> falling_block [关键字参数]`：将区域内的方块转换为下落的方块。
- `/convertblocks <区域> block_display [关键字参数]`：将区域内的方块转换为方块展示实体。
- `//convertblocks falling_block [关键字参数]`：将自己的活动区域内的方块转换为下落的方块。
- `//convertblocks block_display [关键字参数]`：将自己的活动区域内的方块转换为方块展示实体。

## 参数

### `<区域>`

需要影响的[区域](../../arguments/region/zh.md)。对于 `//convertblocks`，不使用此参数，使用命令执行者的活动区域。

### 关键字参数

此命令支持 [`/convertblock`](../convertblock/zh.md) 的所有关键字参数。此外，此命令还支持以下关键字参数：

#### `affect_only`

[方块谓词](/documents/arguments/block_predicate/zh.md)，只影响符合此谓词的方块。

如果没有指定此参数，那么只影响非空气方块，当 `affect_fluid` 为 `false`（即默认情况）时，仅有流体的方块（例如水、熔岩，但不包括含水方块）不会受到影响。

#### `immediately`

布尔值，默认为 `false`。如果区域较大，此命令会缓慢执行，使用 `immediately=true` 可以强制命令立即执行完成，但可能造成卡顿。

#### `bypass_limit`

布尔值，默认为 `false`。当区域可能有 16384 个以上的方块时，为了避免产生大量实体影响性能，命令会拒绝执行，使用 `bypass_limit` 可以强制执行。

#### `unloaded_pos`

和 [`/fill`](../fill/zh.md) 的 `unloaded_pos` 参数相同。

## 示例

- `/convertblocks sphere(5) falling_block nbt={NoGravity:true,Glowing:true}`：将半径 5 个方块的球体区域内的方块转换为没有重力的且发光的下落的方块。
- `//convertblocks block_display`：将活动区域内的方块转换为方块展示实体。