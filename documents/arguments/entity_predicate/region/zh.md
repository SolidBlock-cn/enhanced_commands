# `region`：选择指定区域的实体

此[实体谓词](../zh.md)选择指定的[区域](/documents/arguments/region/zh.md)内的实体。支持使用相对坐标和局部坐标，会根据命令源的位置和朝向（而不是被选择的实体的位置和朝向）来计算。

此实体谓词暂不支持取反。

## 示例

- `@e[region=sphere(5)]`：选择距离命令源为中心。半径为 5 的球体范围内的实体。

## 数据结构

- `type`：此处为 `"enhanced_commands:region"`。
- `region`：[区域](/documents/arguments/region/zh.md)。