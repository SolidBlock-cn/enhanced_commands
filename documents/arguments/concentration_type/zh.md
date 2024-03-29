# 聚合类型

**聚合类型**（concentration type）是一种[参数类型](../zh.md)，指定了当获取到多个实体的数据值，如何处理这多个数值，使之返回一个值。

聚合类型共支持以下值：

- `first`：第一个值。
- `last`：最后一个值。
- `min`：最小值。
- `max`：最大值。
- `average`：平均值。
- `sum`：总和。

当数据只有一个时，无论聚合类型为什么类型，聚合后的结果一定正好是这个值。

当数据为零个时，除了 `sum` 之外，聚合过程会出错。也就是说，只有 `sum` 支持聚合零个值。

以下为使用聚合类型的一些示例：

- `/health get @e average`：获取所有实体的生命值的平均值。
- `/health get @e min`：获取所有实体生命值的最小值。
- `/health get @e max`：获取所有实体生命值的最大值。
- `/health get @e sum`：获取所有实体生命值的总和。
- `/health get @e first`：获取实体选择器匹配到的第一个实体的生命值。
- `/health get @e last`：获取实体选择器匹配到的最后一个实体的生命值。