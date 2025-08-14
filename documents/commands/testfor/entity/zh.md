# `/testfor entity`

此[命令](../../zh.md)用于对实体进行测试，并对选择到的实体应用实体谓词，判断其是否符合实体谓词。如果只选择到一个实体，那么在判断实体谓词过程中，会详细展示其判断原理。

## 语法

- `/testfor entity <实体选择器>`：使用[实体选择器](/documents/arguments/entity_selector/zh.md)选择实体。如果选择到实体，则会显示这些实体的名字和数量。如果实体数量很多，只会显示部分实体的名字，并显示实体的数量。该命令返回的整数值为选择到的实体的数量。
- `/testfor entity <实体选择器> <实体谓词>`：判断选择到的实体是否符合[实体谓词](/documents/arguments/entity_predicate/zh.md)。如果选择到一个实体，则会详细展示该实体谓词的判断过程。如果选择到多个实体，则会显示符合谓词的实体的数量。该命令返回的整数值为符合谓词的实体的数量。

## 示例

- `/testfor entity @s`
- `/testfor entity @e[type=cat]`
- `/testfor entity @s [gamemode=creative, effect=!blindness]`
- `/testfor entity @e[c=5] [block=air]`