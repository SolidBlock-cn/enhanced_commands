# `/stack` 和 `//stack`

此[命令](../zh.md)用于往一个方向多次复制一个区域内的内容。默认情况下，每次复制的位置的差异是该区域在该方向所在轴上的大小。

## 语法

- `/stack <区域> [关键字参数]`
- `/stack <区域> [方向] [关键字参数]`
- `/stack <区域> [数量] [关键字参数]`
- `/stack <区域> [数量] [方向] [关键字参数]`
- `/stack <区域> [数量] vector <x> <y> <z> [关键字参数]`
- `//stack [关键字参数]`
- `//stack [方向] [关键字参数]`
- `//stack [数量] [关键字参数]`
- `//stack [数量] [方向] [关键字参数]`
- `//stack [数量] vector <x> <y> <z> [关键字参数]`

其中，`//stack` 相当于对执行者的活动区域进行操作。

## 参数

### `<区域>`

需要堆叠的[区域](../../arguments/region/zh.md)。使用 `//stack` 时，不指定区域，使用执行者的活动区域。

### `[方向]`

区域复制偏移的[方向](../../arguments/direction/zh.md)。若未指定，则为执行源的朝向的前方，即 `front`。

### `[数量]`

复制的次数，只能是整数。默认为 1。

### `vector <x> <y> <z>`

若使用此参数，则每次复制区域的偏移位置由此向量决定，忽略 `absolute` 和 `gap` 参数。

### 关键字参数

**此命令支持 [`/fill`](../fill/zh.md) 中的所有关键字参数。其中，`unloaded_pos` 参数会同时检测源区域和预计最后一次复制所在的区域是否均在已加载的区块内。此外还支持以下关键字参数：

#### `affect_entities`

实体选择器，如果指定，则会复制符合此实体选择器的实体。玩家不会受到影响。

#### `affect_only`

方块谓词。如果指定，则被复制的位置只有符合此谓词的方块，才会被替换为从源区域复制后的方块。

#### `select`

布尔值。如果为 `true`，则会将执行者的活动区域设置为最后一次复制的区域。

#### `transform_only`

方块谓词。若指定，则源区域中只有符合此谓词的方块才会被复制。

#### `absolute`

布尔值。此参数只在未使用向量表示偏移（即未使用 `vector <x> <y> <z>` 的参数）时存在。若为 `true`，则不会使用区域在该方向所在轴的长度指定每次复制的偏移量。在这种情况下，请指定 `gap` 参数，否则每次复制均没有任何偏移，而是复制到了原先的地方，相当于没有复制。

#### `gap`

整数，默认为 0，可以是负数。此参数只有在未使用向量表示偏移时存在。若指定，则每次偏移量会在区域在复制方向所在轴上的长度的基础上，增加一个值，从而增加每次复制的间隔大小。如果为负数，则意味着每次复制的区域可能会与上次区域有所重叠。对于 `absolute=true` 的情况，此参数则相当于是每次复制的偏移。

> 为了直观理解，我们假设这个区域在所在轴上的大小为 4，用简单的图形描述了复制的位置。X 表示受影响的位置，- 表示未受影响的位置。
>
> 【默认情况】
> ```
> 　　源区域：XXXX----------------
> 第一次复制：----XXXX------------
> 第二次复制：--------XXXX--------
> 第三次复制：------------XXXX----
> ```
>
> 【absolute=true】由于没有指定 gap，每次复制都在源位置进行。
> ```
> 　　源区域：XXXX----------------
> 第一次复制：XXXX----------------
> 第二次复制：XXXX----------------
> 第三次复制：XXXX----------------
> ```
>
> 【gap=1】每次复制均与上一个区域间隔了 1 个方块。
> ```
> 　　源区域：XXXX----------------
> 第一次复制：-----XXXX-----------
> 第二次复制：----------XXXX------
> 第三次复制：---------------XXXX-
> ```
>
> 【gap=-1】每次复制均与上一个区域重叠了 1 个方块。
> ```
> 　　源区域：XXXX----------------
> 第一次复制：---XXXX-------------
> 第二次复制：------XXXX----------
> 第三次复制：---------XXXX-------
> ```
>
> 【absolute=true gap=1】每次复制均与上一个区域偏移了 1 个方块。
> ```
> 　　源区域：XXXX----------------
> 第一次复制：-XXXX---------------
> 第二次复制：--XXXX--------------
> 第三次复制：---XXXX-------------
> ```

## 示例

- `/stack spehre(5)`：将半径为 5 个方块的球体区域内的方块，向命令执行源朝向的前方方向（以下简称前方）复制一次。
- `//stack`：将活动区域的方块向前复制一次。
- `//stack 3`：将活动区域内的方块向前复制三次。
- `//stack 3 up`：将活动区域内的方块向上复制三次。
- `//stack select=true`：复制活动区域，并将活动区域设置为复制后的区域。
- `//stack 2 left select=true`：将活动区域内的方块向左复制两次，并将活动区域设置为复制后的区域。
- `//stack transform_only=!air`：将活动区域内除了空气之外的方向向前复制一次。
- `//stack 3 gap=5`：将活动区域内的方块向前复制一次，每次复制均有 5 个方块的间隔。
- `//stack 3 affect_entities=@e`：将活动区域内的方块和实体向前复制一次。
- `//stack 10000`：此命令有可能会失败，因为预计复制 10000 次后的内容往往在未加载的区域内。
- `//stack 10000 unloaded_pos=break`：将活动区域内的方块向前复制 10000 次，但是遇到未加载的区块时中止执行。
- `//stack 3 vector 5 1 4`：将活动区域内的方块复制三次，每次复制均按照 (5, 1, 4) 的向量进行偏移。