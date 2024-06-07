# `/testarg`

此[命令](../zh.md)用于测试命令的参数类型，以及这些对象的编码和解码过程。

## 语法

- `/testarg block_function <方块函数> [string|nbt|json|string_test|nbt_test|json_test]`：[方块函数](/documents/arguments/block_function/zh.md)。
  - `string`：转化为字符串。默认为此。
  - `nbt`：转化为 NBT。
  - `json`：转化为 JSON。
  - `string_test`：转化为字符串，然后再根据此字符串转化，并检查转化后的结果与原字符串是否一致。
  - `nbt_test`：转化为 NBT，然后再将其转化为对象，并检查转化后的结果与原字符串是否一致。
  - `json_test`：转化为 JSON，然后再将其转化为对象，并检查转化后的结果与原字符串是否一致。
- `/testarg block_predicate <方块谓词> [string|nbt|json|string_test|nbt_test|json_test]`：[方块谓词](/documents/arguments/block_predicate/zh.md)。
- `/testarg nbt <NBT>`：显示 NBT 在格式化渲染后的结果（类似于 `/data get` 命令输出的那种）。
    - `... plainstring`：显示 NBT 直接转换为字符串 NBT（SNBT）后的结果。
    - `... prettyprinted`：显示 NBT 格式化渲染后的结果。
    - `... indented`：显示 NBT 格式化并带有缩进渲染后的结果。
    - `... test`：将 NBT 的字符串形式解析为 [NBT 谓词](/documents/arguments/nbt_predicate/zh.md)和 [NBT 函数](/documents/arguments/nbt_function/zh.md)，并检测 NBT 是否符合此 NBT 谓词，以及 NBT 函数应用的结果是否与此 NBT 相等。
  - `... convert [block_function|block_predicate|region]`：将 NBT 转化为方块函数、方块谓词或区域。
- `/testart nbt_compound [nbt_test|json_test]`：将 NBT 复合标签转化为 NBT 或 JSON 对象，再转换为 NBT，并检测转换前后的结果是否一致。
- `/testarg nbt_function <NBT 函数>`：显示 [NBT 函数](/documents/arguments/nbt_function/zh.md)转换为字符串后的结果。
    - `... apply [NBT]`：显示 NBT 函数应用在空内容或指定的 NBT 后的结果。
    - `... string`：显示 NBT 函数转换为字符串后的结果。
  - `... string_test`：将 NBT 函数转换为字符串，将字符串重新解析为 NBT 函数，并检测重新解析后的 NBT 函数与原先提供的 NBT 函数是否相等。
- `/testarg nbt_predicate <NBT 谓词>`：显示 [NBT 谓词](/documents/arguments/nbt_predicate/zh.md)转换为字符串后的结果。
    - `... match <待测 NBT>`：检测 NBT 是否符合指定的 NBT 谓词。
    - `... string`：显示 NBT 谓词转换为字符串后的结果。
  - `... string_test`：将 NBT 谓词转换为字符串，将字符串重新解析为 NBT 谓词，并检测重新解析后的 NBT 谓词与原先提供的 NBT 谓词是否相等。
- `/testarg pos ...`
    - `... (int_only|prefer_int|prefer_double|double_only) (unchanged|horizontally_centered|centered) <坐标>`：显示指定类型的坐标参数根据命令源（如命令执行者所在的位置）的计算结果。
    - `... (vanilla_vec3|vanilla_vec3_accurate|vanilla_block_pos) <坐标>`：显示指定原版类型的坐标参数根据命令源的计算结果。
- `/testarg region <区域> [string|nbt|json|string_test|nbt_test|json_test]`：[区域](/documents/arguments/region/zh.md)。
- `/testart region <区域> illustrate`：使用玻璃绘制此区域，并检测此区域是否有异常。如果区域附近有方块坐标在迭代区域的方块坐标时未被包含，却被判定为在此区域内，将用橙色染色玻璃表示。如果迭代过程包含的区域方块坐标中，有部分方块坐标被判定为不在区域内，将用红色染色玻璃表示。正常情况下，不应该产生任何的红色染色玻璃和橙色染色玻璃。