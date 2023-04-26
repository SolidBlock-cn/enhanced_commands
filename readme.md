# 增强命令模组
注意：模组名称只是暂时的，未来可能会改变。模组目前还在开发中，目前并不稳定。

模组目前只支持 1.19.4，未来会支持 1.19.3、1.19.2 和 1.18.2。只支持 Fabric。没有支持 Forge 以及其他 Minecraft 版本的计划。

## 已实现的功能
### 命令
`/execute` 的各个子命令现在已经独立，但原来的写法仍然有效。如 `/execute as @e at @s run summon creeper` 可以写成 `/as @e at @s summon creeper`。

此外，还加入了以下内容：
- `/if ...` 和 `/unless ...`（下面均省略 `/unless`）
  - `/if block` 和原版的类似，但是支持本模组的增强的[方块谓词](#方块谓词)。
  - `/if rand <概率>` 仅有一定的概率通过。
- `/testfor ...`
  - `/testfor block <坐标> [方块谓词]`：检测一个方块是否符合[谓词](#方块谓词)。当没有指定谓词的时候，显示一个方块的名称、ID 和属性。指定的谓词的时候，会使用文字描述该方块是否符合谓词。
  - `/testfor block_info <坐标> ...`：检测某一个方块的数据，目前支持以下值：
    - `... hardness [倍率=1]`：方块的坚硬度
    - `... luminance`：方块的发光度
    - `... (strong_redstone_power|weak_redstone_power) <方向>`：方块在某一侧拥有的红石能量
    - `... (light|block_light|sky_light)`：光照等级
    - `... emits_redstone_power`：是否产生红石能量（返回的数值为 0 和 1，下同）
    - `... opaque`：是否为不透明方块
    - `... model_offset`：是否存在模型偏移，如有，会显示模型偏移的值
    - `... suffocate`：是否能够窒息
    - `... block_vision`：是否能够阻挡实体的视线
    - `... replaceable`：是否可替换
    - `... random_ticks`：是否存在随机刻
- `/rand ...`：用于生成随机数
  - `/rand` 同 `/rand float`
  - `/rand boolean [为 true 的概率]`：生成随机的 true 或 false，可以指定生成 true 的概率（必须是在 [0,1] 之间）
  - `/rand float`：生成 0 到 1 之间的随机数
  - `/rand float [最小值=0] <最大值>`：生成随机数
  - `/rand int`：生成 0 到 15 之间的随机整数
  - `/rand int [最小值=0] <最大值>`：生成随机整数
  - 最小值不能小于 0，最大值不能小于最小值（但是可以相等）

### 参数类型
#### 方块谓词

支持以下写法：
- 方块 id，如 `minecraft:stone`
- 方块属性，如：
  - `wooden_stairs[facing=east]`
  - `wooden_stairs[facing!=east]`（属性 facing 的值不能为 east）
  - `wooden_stairs[facing=*]`（属性 facing 必须存在，无论其值）
  - `wooden_stairs[facing!=*]`（属性 facing 不能存在）
  - `wooden_stairs[facing>east]`（属性 facing 的值在枚举常量中必须在 east 的后面，类似地，也可以使用 `<`、`>=`、`<=`
- 方块标签，如 `#minecraft:stairs`
  - 方块标签也支持属性，和上面的语法相同，不过在解析的时候不会核验属性是否存在，运行的时候会检测是否在使用这个名称的标签，而不是像上面的局部那样直接检测具体的属性对象。
- 否定，如 `!minecraft:stone`（任何不是石头的方块）
- 位置偏移，如 `>minecraft:stone`（位于石头上面的方块）、`<minecraft:stone`（位于石头下面的方块）
- 多个条件的交集：`all(方块谓词1, 方块谓词2, ...)`；若参数为空，即 `all()`，则始终通过
- 多个条件的并集：`any(方块谓词1, 方块谓词2, ...)`；若参数为空，即 `any()`，则始终失败
- 两个条件必须同时满足或同时不满足：`same(方块谓词1, 方块谓词2)`
- 两个条件必须一个满足、另一个不满足：`diff(方块谓词1, 方块谓词2)`
- 只有在指定的概率下满足：`rand(概率值)`

上面各种格式可以复合，例如：
- `>any(stone, #slabs[waterlogged=false], !<air)`
- `!>>diff(rand(0.2), wooden_stairs)`
- `any(all(#weather_immune, #stairs), #infiniburn_end, stone)`