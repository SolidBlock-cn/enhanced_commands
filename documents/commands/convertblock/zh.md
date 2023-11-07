# `/convertblock`

此[命令](../zh.md)可以将特定位置的方块转换为实体。

## 语法

- `/convertblock <坐标> falling_block [关键字参数]`：将方块转换为下落的方块。
- `/convertblock <坐标> block_display [关键字参数]`：将方块转换为方块展示实体。

## 参数

### `<坐标>`

需要影响的方块的[坐标](/documents/arguments/pos/zh.md)。如果是未加载的坐标则会执行失败。

### 关键字参数

此命令支持以下关键字参数： `skip_light_update`、`notify_listeners`、`notify_neighbors`、`force_state`、`suppress_initial_check`、`suppress_replaced_check`、`force`，其用法与 [`/fill`](../fill/zh.md) 中的参数相同。但是与 `/fill` 不同的是，`/convertblock` 的 `force_state` 的默认值为 `true`，而不是 `false`。

此命令还支持以下关键字参数：

#### `nbt`

[NBT 函数](/documents/arguments/nbt_function/zh.md)，用于影响转换后的实体的 NBT。

#### `affect_fluid`

布尔值，默认为 `false`。当为 `false` 时，对于含有流体的方块，只会转换其非流体的部分（例如 `waterlogged=true` 的方块，转换时会变成 `waterlogged=false`），并留下对应的流体方块（例如水源）。当为 `true` 时，会连同流体一起转换，但请注意流体在下落的方块、方块展示等实体中是无法正常显示的。

## 示例

- `/convertblock ~~-1~ block_display`：将下方 1 格的方块转换为方块展示实体。
- `/convertblock ~~-1~ falling_block nbt={OnGround:true}`：将下方 1 格的方块转换为下落的方块，并禁止其通过下落转换为普通的方块。