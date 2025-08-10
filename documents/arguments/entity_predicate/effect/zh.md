# `effect`：检测实体的状态效果

此[实体谓词](../zh.md)可用于检测实体拥有的状态效果。可以指定一个状态效果，也可以指定多个状态效果。

## 语法

- `[effect=<状态效果>]`：当实体拥有此状态效果时通过。这里的状态效果为 id，例如 `absorption`。
- `[effect=!<状态效果>]`：当实体没有此状态效果时通过。
- `[effect={<状态效果 1>=<布尔值 1>, <状态效果 2>=<布尔值 2>, ...]`：当实体的各个状态效果符合指定的布尔值时通过。布尔值为 `true` 和 `false`，可以添加感叹号以取反，`!true` 相当于 `false`，`!false` 相当于 `true`。

## 示例

- `@a[effect=night_vision]`：所有拥有夜视效果的玩家。
- `@a[effect=strength, effect=speed]`：所有拥有力量和速度效果的玩家。
- `@a[effect={strength=true, effect=true}]`：等价于上一个例子。
- `@a[effect=!fire_resistance]`：所有没有抗火效果的玩家。
- `@a[effect={fire_resistance=false}]`：等价于上一个例子。
- `@a[effect={fire_resistance=!true}]`：等价于上一个例子。
- `@a[effect=!blindness, effect=night_vision]`：所有没有失明效果但有夜视效果的玩家。
- `@a[effect={blindness=false, night_vision=true}]`：等价于上一个例子。

## 数据结构

当直接指定单个效果时：

- `type`：此时为 `"enhanced_commands:effect"`。
- `effect`：字符串，效果的 id。
- `inverted`：布尔值，可选，默认为 `false`。

当使用效果与布尔值的映射时：

- `type`：此时为 `"enhanced_commands:effects"`。
- `effects`：列表。
    - 列表的元素。
        - `effect`：字符串，效果的 id。
        - `data`：映射。与战利品表的谓词中的效果数据的数据结构相当。
            - `amplifier`：整数范围，可选。
            - `duration`：整数范围，可选。
            - `ambient`：布尔值。可选。
            - `visible`：布尔值。可选。
        - `expected`：布尔值。默认为 `true`。