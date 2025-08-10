# `alternatives`：多个实体谓词的并集

此[实体谓词](../zh.md)指定多个实体谓词，当实体符合这组谓词中的任意一个时通过。该参数的值需要用中括号括起来，里面是多个实体谓词，多个实体谓词用逗号隔开。此参数支持取反。

## 示例

- `@e[alternatives=[[type=cow], [type=sheep]]]`：选择所有的牛和羊。
- `@a[alternatives=[Player1, Player2]]`：选择 Player1 和 Player2。
- `@e[type=sheep, alternatives=[[tag=a], [tag=b]]]`：在所有的羊中，选择有 a 或 b 标签的。
- `@e[type=sheep, alternatives=![[tag=a], [tag=b]]]`：在所有的羊中，选择既没有 a 也没有 b 标签的。

## 数据结构

- `type`：此时为 `"enhanced_commands:alternatives"`。
- `predicates`：列表。
    - 一个[实体谓词](../zh.md)。
- `inverted`：布尔值，可选，默认为 false。