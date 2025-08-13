# `/if|unless blockinfo`

这个[命令](../../zh.md)用于在只有当方块信息符合（或不符合）指定的条件下才执行命令。

## 语法

- `/if|unless blockinfo <坐标> hardness <浮点数范围> ...`
- `/if|unless blockinfo <坐标> luminance <整数范围> ...`
- `/if|unless blockinfo <坐标> strong_redstone_power <方向> <整数范围> ...`
- `/if|unless blockinfo <坐标> weak_redstone_power <方向> <整数范围> ...`
- `/if|unless blockinfo <坐标> light <整数范围> ...`
- `/if|unless blockinfo <坐标> block_light <整数范围> ...`
- `/if|unless blockinfo <坐标> sky_light <整数范围> ...`
- `/if|unless blockinfo <坐标> emits_redstone_power ...`
- `/if|unless blockinfo <坐标> opaque ...`
- `/if|unless blockinfo <坐标> model_offset ...`
- `/if|unless blockinfo <坐标> suffocate ...`
- `/if|unless blockinfo <坐标> block_vision ...`
- `/if|unless blockinfo <坐标> replaceable ...`
- `/if|unless blockinfo <坐标> random_ticks ...`

上述命令参数完成后，后面可以接一个完整的命令。

上述各命令的含义参见 [`/testfor blockinfo`](/documents/commands/testfor/blockinfo/zh.md)。

## 参数

### `<坐标>`

[坐标](/documents/arguments/pos/zh.md)。需要测试的方块的坐标。

## 示例

- `/if blockinfo ~ ~ ~ light 6.. tellraw @a "此处有光"`
- `/if blockinfo ~ ~ ~ replaceable setblock ~ ~ ~ stone`
- `/unless blockinfo ~ ~-1 ~ luminance 10.. setblock ~ ~-1 ~ glowstone`

## 参见

- [`/testfor blockinfo` 命令](/documents/commands/testfor/blockinfo/zh.md)