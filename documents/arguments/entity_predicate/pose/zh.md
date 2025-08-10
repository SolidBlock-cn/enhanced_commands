# `pose`：测试实体的姿势

此[实体谓词](../zh.md)选择指定姿势的实体，支持取反。

支持的姿势名称包括：

- `standing`
- `fall_flying`
- `sleeping`
- `swimming`
- `spin_attack`
- `crouching`
- `long_jumping`
- `dying`
- `croaking`
- `using_tongue`
- `sitting`
- `roaring`
- `sniffing`
- `emerging`
- `digging`

## 示例

- `@e[pose=crouching]`：选择所有正在爬行的实体。
- `@a[pose=!swimming]`：选择所有未在游泳的实体。

## 数据结构

- `type`：此处为 `"enhanced_commands:pose"`。
- `pose`：字符串。
- `inverted`：布尔值，可选，默认为 `false`。