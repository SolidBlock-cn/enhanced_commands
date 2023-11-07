本模组加入了一系列增强的**命令**。

## 区域命令

- [`/activeregion`](activeregion/zh.md) 或 `/ar` 以及 `//activeregion` 和 `//ar`：管理玩家的活动区域。
- [`/regionselection`](regionselection/zh.md) 和 `/rs`：管理玩家的区域选择的类型。

## 方块命令

这些命令用于对一个区域内的方块（有时包括实体）进行操作。

- [`/convertblock`](convertblock/zh.md)：将方块转换为特定的实体（如展示实体、下落的方块）。
- [`/convertblocks`](convertblocks/zh.md) 和 `//convertblocks`：将区域内的方块转换为特定的实体。
- [`/draw`](draw/zh.md)：在世界内绘制一条曲线。
- [`/fill`](fill/zh.md)、`//fill` 和 `//f`：设置区域内的方块。
- [`/mirror`](mirror/zh.md) 和 `//mirror`：镜像（翻转）区域内的方块和实体。
- [`/move`](move/zh.md) 和 `//move`：移动区域内的方块和实体。
- [`/outline`](outline/zh.md)：填充区域边缘位置的方块，可以一并填充区域边缘以内的方块。
- [`/replace`](replace/zh.md) 和 `//replace`：替换区域内的符合指定谓词的方块。
- [`/rotate`](rotate/zh.md) 和 `//rotate`：旋转区域内的方块和实体。
- [`/stack`](stack/zh.md) 和 `//stack`：向一个方向多次复制区域。
- [`/wall`](wall/zh.md)：填充区域内的四周（墙面）的方块，可一一并填充区域四周以内的方块。

## 实体命令

- [`/tprel`](tprel/zh.md)：传送实体，其中坐标和旋转均是以被传送实体为基础计算的。

## 测试命令

- [`/testarg`](testarg/zh.md)：对特定类型的参数进行测试。
- [`/testfor`](testfor/zh.md)：测试和获取当前世界中的数据。

## 独立的 `/execute` 子命令

`/execute` 的各个子命令已经独立为单独的命令（原先的语法依然可以正常使用）。如 `/execute as @e at @s run summon creeper` 可以写成 `/as @e at @s summon creeper`。

`/execute` 的各个已经独立的子命令共包括：

- 条件子命令：
    - `/if`
    - `/unless`
- 修饰子命令：
    - `/as`
    - `/at`
    - `/positioned`
    - `/rotated`
    - `/facing`
    - `/align`
    - `/anchored`
    - `/in`
    - `/summon`
    - [`/silenced`](silenced/zh.md)（本模组添加）：执行命令，但是不会在聊天或者控制台产生任何反馈。
    - [`/for_region`](for_region/zh.md)（本模组添加）
    - [`/for`](for/zh.md)（本模组添加）
- 存储子命令：
    - `/store`

## 简化的 `/gamemode` 命令

各个游戏模式的命令均添加了简化的版本。

- `/gmc [玩家]` 相当于 `/gamemode creative [玩家]`
- `/gms [玩家]` 相当于 `/gamemode survival [玩家]`
- `/gma [玩家]` 相当于 `/gamemode adventure [玩家]`
- `/gmsp [玩家]` 相当于 `/gamemode spectator [玩家]`

## 其他命令

- [`/tasks`](tasks/zh.md)：管理当前服务器中的任务。
- [`/rand`](rand/zh.md)：生成随机数。