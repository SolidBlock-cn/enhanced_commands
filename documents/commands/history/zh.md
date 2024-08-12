# `/history`

此[命令](../zh.md)用于查看或清除玩家或服务器的操作历史记录，这些历史记录可通过 `/undo` 或 `/redo` 来撤销。

## 语法

- `/history clear [关键字参数：target | target-server | type]`：清除操作历史记录
- `/history list [关键字参数：target | target-server | type | from | limit | sort]`：显示操作历史记录
- `/history`：效果同 `/history list`。

## 参数

### 关键字参数

#### `target`

实体选择器，只能选择单个玩家。查看或清除该玩家的历史记录。

#### `target-server`

布尔值，默认为 `false`。如果为 `true`，将查看或清除服务器的历史记录。

#### `type`

字符串，可接受的值为 `undo` 和 `redo`，默认为 `undo`。值的意义如下：

- `undo`：查看或清除由各具体操作或 `/redo` 产生的、可由 `/undo` 命令撤销的历史记录。
- `redo`：查看或清除由 `/undo` 产生的、可由 `/redo` 命令撤销的历史记录。

#### `from`

整数，最小值为 0，默认为 0。列举的操作历史记录的首项。

#### `limit`

整数，最小值为 0，最大值为 50，默认为 7。一页显示的操作历史记录的个数。

当列举未尽时，会提供按钮，以显示上一页或下一页的操作历史记录。

#### `sort`

字符串，可接受的值为 `latest` 和 `oldest`。默认为 `latest`。值的意义如下：

- `latest`：最近的操作记录排在前。
- `oldest`：最晚的操作记录排在前。

## 示例

- `/history`：列举自己的所有可通过 `/undo` 撤销的历史记录。
- `/history list`：列举自己的所有可通过 `/undo` 撤销的历史记录。
- `/history list target=Alex`：列举玩家 Alex 的所有可通过 `/undo` 撤销的历史记录。
- `/history list target-server=true type=redo`：列举服务器的所有可通过 `/redo` 撤销的历史记录。
- `/history list limit=5`：列举最近的 5 个历史记录。
- `/history list limit=15 sort=oldest`：列举最早的 15 个历史记录。
- `/history list from=5 limit=5`：列举最近的第 6 个至第 10 个历史记录。
- `/history clear`：清除自己的可通过 `/undo` 操作的历史记录。
