# `idreplace()`

此函数用于对方块 ID 进行替换。当替换后的 ID 存在时，则使用这个方块（但不会保留方块状态属性），否则不执行操作。

## 语法

`idreplace(<正则表达式>, <替换内容>)`

## 示例

- `idreplace(red, yellow)`：尝试将所有 ID 含有“red”的方块替换为“yellow”，例如红色羊毛替换为黄色羊毛，红色玻璃板替换为黄色玻璃板。替换后将使用默认的方块状态，不会保留属性。
- `idreplace(red, yellow)[~]`：尝试将所有 ID 含有“red”的方块替换为“yellow”，并尽可能保留所有的方块状态属性。