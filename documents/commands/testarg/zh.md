# `/testarg`

此[命令](../zh.md)用于对命令的参数类型进行测试。

## 语法

- `/testarg block_function <方块函数>`：显示[方块函数](/documents/arguments/block_function/zh.md)转化为字符串和 NBT 后的结果。
    - `... string`：显示方块函数转化为字符串后的结果。
    - `... nbt`：显示方块函数转化为 NBT 后的结果。
    - `... reparse`：将方块函数转化为字符串后重新解析，并检查重新解析后的方块函数是否与原来的保持一致。
    - `... redeserialize`：将方块函数序列化为 NBT 后重新反序列化，并检查反序列化后的方块函数是否与原来的保持一致。
- `/testarg block_predicate <方块谓词>`：显示[方块谓词](/documents/arguments/block_predicate/zh.md)转化为字符串和 NBT 后的结果。
    - `... string`：显示方块谓词转化为字符串后的结果。
    - `... nbt`：显示方块谓词转化为 NBT 后的结果。
    - `... reparse`：将方块谓词转化为字符串后重新解析，并检查重新解析后的方块函数是否与原来的保持一致。
    - `... redeserialize`：将方块谓词序列化为 NBT 后重新反序列化，并检查反序列化后的方块函数是否与原来的保持一致。
- `/testarg nbt <NBT>`：显示 NBT 在格式化渲染后的结果（类似于 `/data get` 命令输出的那种）。
    - `... plainstring`：显示 NBT 直接转换为字符串 NBT（SNBT）后的结果。
    - `... prettyprinted`：显示 NBT 格式化渲染后的结果。
    - `... indented`：显示 NBT 格式化并带有缩进渲染后的结果。
    - `... test`：将 NBT 的字符串形式解析为 [NBT 谓词](/documents/arguments/nbt_predicate/zh.md)和 [NBT 函数](/documents/arguments/nbt_function/zh.md)，并检测 NBT 是否符合此 NBT 谓词，以及 NBT 函数应用的结果是否与此 NBT 相等。
- `/testarg nbt_function <NBT 函数>`：显示 [NBT 函数](/documents/arguments/nbt_function/zh.md)转换为字符串后的结果。
    - `... apply [NBT]`：显示 NBT 函数应用在空内容或指定的 NBT 后的结果。
    - `... string`：显示 NBT 函数转换为字符串后的结果。
    - `... reparse`：将 NBT 函数转换为字符串，将字符串重新解析为 NBT 函数，并检测重新解析后的 NBT 函数与原先提供的 NBT 函数是否相等。
- `/testarg nbt_predicate <NBT 谓词>`：显示 [NBT 谓词](/documents/arguments/nbt_predicate/zh.md)转换为字符串后的结果。
    - `... match <待测 NBT>`：检测 NBT 是否符合指定的 NBT 谓词。
    - `... string`：显示 NBT 谓词转换为字符串后的结果。
    - `... reparse`：将 NBT 谓词转换为字符串，将字符串重新解析为 NBT 谓词，并检测重新解析后的 NBT 谓词与原先提供的 NBT 谓词是否相等。
- `/testarg pos ...`
    - `... (int_only|prefer_int|prefer_double|double_only) (unchanged|horizontally_centered|centered) <坐标>`：显示指定类型的坐标参数根据命令源（如命令执行者所在的位置）的计算结果。
    - `... (vanilla_vec3|vanilla_vec3_accurate|vanilla_block_pos) <坐标>`：显示指定原版类型的坐标参数根据命令源的计算结果。
- `/testarg region <区域> ...`
    - `... string`：显示区域转换为字符串后的结果。
    - `... nbt`：显示区域转化为 NBT 后的结果。
    - `... reparse`：将区域转化为字符串后重新解析，并检查重新解析后的区域是否与原来的保持一致。
    - `... redeserialize`：将区域序列化为 NBT 后重新反序列化，并检查反序列化后的方块函数是否与原来的保持一致。
    - `... illustrate`：使用玻璃绘制此区域，并检测此区域是否有异常。如果区域附近有方块坐标在迭代区域的方块坐标时未被包含，却被判定为在此区域内，将用橙色染色玻璃表示。如果迭代过程包含的区域方块坐标中，有部分方块坐标被判定为不在区域内，将用红色染色玻璃表示。正常情况下，不应该产生任何的红色染色玻璃和橙色染色玻璃。