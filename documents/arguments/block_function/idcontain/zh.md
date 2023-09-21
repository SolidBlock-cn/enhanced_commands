# `idcontain()`

随机选择一个 ID 含有指定的正则表达式的方块。

## 语法

`idcontain(<正则表达式>)`

其中正则表达式即使拥有 `$`、`^`、`?` 等字符，也不需要引号。但是如果含有括号、引号或者其他的字符，则仍需要引号。

## 示例

- `idcontain(_terracotta$)`：随机选择一个 ID 含有“terracotta”结尾的方块，也就是陶瓦和带釉陶瓦。
- `idcontain(red)`：随机选择一个 ID 含有“red”的方块。
- `idcontain(*)`：无效的正则表达式，因此无法解析。

## 参见

- [方块谓词 `idcontain()`](../../block_predicate/idcontain/zh.md)