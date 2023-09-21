# `/tasks`

此命令用于管理当前服务器的计划任务。

使用 `/setblocks` 或 `/draw` 命令产生大量方块，且没有设置 `immediately=true` 时，就会产生计划任务，从而将一个任务分到多个刻内完成。

## 语法

- `/tasks` 相当于 `/tasks count`
- `/tasks count`：返回当前的计划任务的个数
- `/tasks remove`：安全地结束当前的计划任务