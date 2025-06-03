# `/nbt`：随心所欲地操作 NBT

此[命令](../zh.md)可用于查询或修改 NBT 数据，包括方块、实体和存储的 NBT 数据。

## 语法

> 带有“*”号的命令还未实现。

- `/nbt get <来源> [路径] [关键字参数]`：获取指定来源的 NBT：
- `/nbt set <目标> <路径> <NBT 函数>`：设置指定目标的 NBT 为指定的 NBT 函数的结果。
- `/nbt list (prepend|append) <目标> <路径> <NBT 函数>`：在指定列表或数组的开始或末尾加入元素。
- `/nbt list insert <目标> <路径> <NBT 函数>`：在指定的目标在指定路径的列表的特定元素前添加元素。
- `/nbt merge <目标> <NBT 函数>`：合并指定目标的 NBT。
- `/nbt replace <目标> <NBT 谓词> <NBT 函数>`：将指定目标的 NBT 中的任何符合谓词的部分均应用函数。
- `/nbt string replace <目标> <路径> <目标内容> <替换内容> [关键字参数：lenient | recursive]`
- `/nbt string substring <目标> <路径> <起始索引> [结束索引] [关键字参数：lenient]`
- `/nbt regex replace <目标> <路径> <正则表达式> <替换内容> [关键字参数：lenient | original]`
- `/nbt regex split <目标> <路径> <正则表达式> [关键字参数：lenient]`*
- `/nbt list shuffle <目标> <路径>`：将指定目标的 NBT 在指定路径的列表打乱顺序。*
- `/nbt list reverse <目标> <路径>`：将指定目标的 NBT 在指定路径的列表反向。*
- `/nbt calc <目标> <路径>`：将指定目标的 NBT 的指定路径的值进行计算，并将该路径的值设置为计算后的值。*
- `/nbt remove <目标> <路径>`：将指定目标的 NBT 的指定路径的部分移除。*