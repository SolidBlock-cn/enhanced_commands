# `postprocess()`：解决您的栅栏和墙

此[方块函数](../zh.md)可对方块进行后处理，例如进行方块更新，这可修复栅栏与墙的连接问题。

如需演示此函数的功能，您依次可执行以下命令进行测试（可以自行添加参数以调整执行效果）：

```
/setblocks outwards(5 0) #walls[waterlogged=false, *] force=true
/setblocks outwards(5 0) postprocess()
```

## 语法

- `postprocess()`：在所有方向上进行后处理。
- `postprocess(all)`：在所有方向上进行后处理。
- `postprocess(horizontal)`：在水平方向上进行后处理。
- `postprocess(vertical)`：在竖直方向上进行后处理。
- `postprocess(<方向> ...)`：在指定方向上进行后处理。

## 参数

### `<方向> ...`

方向。如果指定多个方向，则用空格分开。

## 示例

- `postprocess()`：在所有方向上进行后处理（解决所有方向上的连接问题）。
- `postprocess(east)`：在东方向上进行后处理（例如修复东测的连接问题）。

## 参见

- [`/postprocess` 命令](/documents/commands/postprocess/zh.md)

## 数据结构

- `type`：当前为 `"enhanced_commands:post_process"`。
- `directions`：列表。
    - 列表的元素。字符串。