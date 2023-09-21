# `idcontain()`

此[方块谓词](../zh.md)用于检测方块的 ID 是否包含指定的正则表达式。

## 语法

`idcontain(<正则表达式>)`

其中正则表达式即使拥有 `$`、`^`、`?` 等字符，也不需要引号。但是如果含有括号、引号或者其他的字符，则仍需要引号。

## 示例

- `idcontain(_terracotta$)`：匹配所有 ID 以“terracotta”结尾的方块，也就是所有的陶瓦和带釉陶瓦。
- `idcontain(red)`：匹配所有 ID 含有“red”的方块，包括“redstone”等词语中的“red”。
- `idcontain(*)`：无效的正则表达式，因此无法解析。

## 参见

- [方块函数 `idcontain()`](../../block_function/idcontain/zh.md)