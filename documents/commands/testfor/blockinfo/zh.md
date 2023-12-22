# `/testfor block_info`

此[命令](../../zh.md)用于获取世界内指定位置的方块的一些信息，以及对这些信息进行测试。

## 语法

- `/testfor blockinfo <坐标> ...`：检测指定位置的方块的……
    - `... hardness [倍率]`：硬度；返回的值会乘以 `[倍率]`，默认为 1。
    - `... luminance`：发光强度。仅限方块自身的发光。
    - `... strong_redstone_power <方向>`：指定[方向](/documents/arguments/direction/zh.md)上的强红石能量。
    - `... weak_redstone_power <方向>`：指定方向上的弱红石能量。
    - `... light`：光照级别。
    - `... block_light`：方块光照级别，即由发光方块导致的光照。
    - `... sky_light`：天空光照级别，即由天空（太阳或月亮）导致的光照。
    - `... emits_redstone_power`：是否会放出红石信号。
    - `... opaque`：是否为不透明方块。
    - `... model_offset`：模型偏移。偏移的值会显示在聊天信息中，返回的值为 1（有偏移）或 0（无偏移）。
    - `... suffocate`：是否会导致窒息。
    - `... block_vision`：是否会阻碍实体的视线。
    - `... replaceable`：放置方块时能否被替换。
    - `... random_ticks`：是否存在随机刻。

## 参数

### `<坐标>`

需要检测的[方块坐标](/documents/arguments/pos/zh.md)。

坐标可以超出世界的界限以及建筑限制，但是必须是已加载的区块内，否则会抛出错误。建筑限制外的方块通常是虚空空气。

## 示例

- `/testfor blockinfo 1 2 3`：获取 (1, 2, 3) 位置的方块的硬度。
- `/testfor blockinfo ~~-1~ strong_redstone_power up`：执行源的位置下方一格位置的方块是否存在往上的强红石信号。

## 参见

- [`/testfor`](../zh.md)