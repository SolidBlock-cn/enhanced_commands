# `predicate()`

判断方块是否符合指定的战利品表谓词。可以使用战利品表谓词的 id，也可以直接使用 json 提供战利品表。

如果使用战利品表 json，那么参数解析时就会直接完全解析战利品表 json。如果是 id，那么会在实际对方块进行测试时，获取该 id 的战利品表谓词（战利品表谓词由数据包定义）。如果该 id 的战利品表谓词不存在，则始终不通过（不抛出错误）。

战利品表谓词用作方块谓词时只能测试方块，不能测试命令源的实体。

## 语法

- `predicate(<战利品表谓词 id>)`
- `predicate(<战利品表谓词 json>)`

如果使用 json，那么 json 在解析过程中比原版更加宽松，支持不带引号的字符串。

## 示例

- `predicate({condition: block_state_property, block: "minecraft:air"})`
- `predicate({condition: block_state_property, block: "minecraft:oak_stairs", properties: {waterlogged: "false"}})`