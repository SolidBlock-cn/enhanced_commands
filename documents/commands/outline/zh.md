# `/outline` 和 `//outline`

此[命令](../zh.md)用于设置一个区域的边缘位置的方块，并且可以同时设置边缘以内的位置的方块。

## 语法

- `/outline <区域> <方块> [边缘类型] [关键字参数]`
- `//outline <方块> [边缘类型] [关键字参数]`

此命令在效果上等价于 `/fill outline(<区域>, <边缘类型>) <方块>`。

## 参数

### `<方块>`

方块函数。需要填充区域的边缘位置的方块。

### `[边缘类型]`

枚举。可用的值可参考区域类型 [`region()`](/documents/arguments/region/outline/zh.md)。默认为 `outline`。

### 关键字参数

此命令支持 `/fill` 的所有关键字参数外，还支持以下关键字参数：

#### `inner`

方块函数。填充区域边缘以内的方块。如果未指定，则不影响区域边缘以内的方块。如果指定了此参数，则命令在效果上等价于 `/fill <区域> if(region(outline<区域>), <方块>, <inner>)`。

## 示例

- `/outline sphere(5) yellow_wool`：使用黄色羊毛绘制半径为 5 个方块的空心球体。
- `//outline red_wool`：使用红色羊毛填充活动区域的边缘位置。
- `//outilne red_wool outline inner=white_wool`：使用红色羊毛填充活动区域的边缘位置，非边缘位置使用白色羊毛填充。