# `/if|unless rand`

此[命令](../../zh.md)用于在特定的概率下执行或者不要执行命令。

## 语法

- `/if|unless rand <概率> ...`

## 参数

### `<概率>`

浮点数。条件符合的概率。

## 示例

- `/if rand 0.1 summon creeper`
- `/unless rand 0.1 setblock ~~~ lava`