本模组加入了一系列增强的**命令**。

## 方块命令

这些命令用于放置方块。

- [`/draw`](draw/zh.md)：在世界内绘制一条曲线。
- [`/replace`](replace/zh.md)：替换一个区域内的符合指定谓词的方块。
- [`/setblocks`](setblocks/zh.md)：设置区域内的方块。

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
    - [`/silenced`](../silenced/zh.md)（本模组添加）：执行命令，但是不会在聊天或者控制台产生任何反馈。
    - [`/for_region`](../for_region/zh.md)（本模组添加）
    - [`/for`](../for/zh.md)（本模组添加）
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