# `enchant()` 物品函数：随心所欲修改物品魔咒

此[物品函数](../zh.md)可用于修改物品魔咒。

## 语法

- `enchant(<魔咒修改>, ...)`

此物品函数可以指定不限个数的魔咒修改。

## 参数

### `<魔咒修改>`

一个魔咒修改拥有以下语法：

- `<魔咒修改目标> [魔咒等级提供器] [选项]`：直接给予魔咒。例如：
    - `sharpness`：给予等级为 1 的锋利。
    - `sharpness 3`：给予等级为 3 的锋利。
    - `any of sharpness|durability 3`：给予等级为 3 的锋利或耐久（随机选择一个）。
    - `all of sharpness|durability 5`：给予等级为 5 的锋利和耐久（两个都有）。
    - `any_supported random_possible`：给予随机的一个受支持的附魔，等级为合理范围内的随机等级。
    - `all_supported max_reasonable`：给予所有受支持的魔咒（不考虑魔咒冲突），等级为各魔咒的最高合理等级。
- `!<魔咒修改目标>`：移除魔咒，例如：
    - `!all`：移除所有魔咒。
    - `!silk_touch`：移除精准采集魔咒。
    - `!any of power|flame`：移除力量和火矢中的随机一个魔咒。
    - `!all of power|flame`：移除力量和火矢中魔咒（两个都移除）。
    - `!any_supported`：移除随机的一个受支持的魔咒（可能本就不存在于物品中）。
    - `!all_present`：移除物品现有的所有魔咒。
- `natual <数值提供器> [物品标签]`：给予随机受支持的非宝藏魔咒（可能是多个），类似于原版的附魔台或自然生成的魔咒。
    - `natual 30`：给予相当于有 30 个书架的附魔台给予的魔咒。
    - `natual 25..30`：给予相当于有 25~30 个（随机数）书架的附魔台给予的魔咒。

其中，`<魔咒修改目标>` 的语法如下：

- 魔咒 ID 或魔咒标签，如果是多个，用 `|` 隔开（将会从这多个项中任选一个），例如：
    - `sharpness`
    - `sharpness|durability`
    - `#curse`：从此魔咒标签中任选一个
    - `silk_touch|#curse`：从精准采集以及所有拥有 `#curse` 标签的魔咒中任选一个。
    - `#curse|#smelts_loot`：从拥有 `#curse` 或 `#smelts_loot` 的所有魔咒中任选一个。
- `all of <魔咒 ID 或魔咒标签>`：表示多个魔咒。例如：
    - `all of #curse`
    - `all of sulk_touch|#curse`
    - `all of #tradeable|#treasure`
- `any of <魔咒 ID 或魔咒标签>`：表示从多个魔咒中随机选择一个，效果等同于省略前面的“`any of`”。例如：
    - `any of sharpness|durability`
    - `any of silk_touch|#curse`
    - `any of #curse|#smelts_loot`
- `all`：所有魔咒。
- `any`：随机选择一个魔咒。
- `all_supported`：所有支持的魔咒。
- `any_supported`：随机选择一个支持的魔咒。
- `all_present`：所有物品现有的魔咒。
- `any_present`：随机选择一个物品现有的魔咒。

`<魔咒等级提供器>` 的语法如下：

- `<数值提供器>`：直接由数值提供器确定魔咒等级。
- `upgrade <数值提供器>`：如果已有魔咒，则在已有魔咒的基础上升级。如果没有此魔咒，则在 0 级的基础上升级。
- `random_reasonable`：魔咒的合理等级范围内的随机值。
- `random_possible`：所有可能的魔咒等级（0 到 255）中的随机值。
- `max_reasonable`：魔咒的最高合理等级。
- `max_possible`：魔咒的最高可能的等级，即 255。
- `upgrade_randomly`：魔咒已有等级与最高等级之间的随机值。如果魔咒已有等级超过最高等级，则不作变动。
- `degrade_randomly`：魔咒已有等级与最低等级之间的随机值。如果魔咒已有等级低于最低等级，则不作变动。

`[选项]` 支持以下值：

- `-c`：始终将魔咒的等级限制在合理范围等级以内（超过最高合理等级的，取最高合理等级，低于最低合理等级的，取最低合理等级）。例如，效率的最高等级为 5 级，因为 `efficiency 7 -c` 相当于给予等级为 5 的效率。
- `-s`：只在魔咒受支持且不冲突的情况下给予魔咒。例如，`all of smite|bane_of_arthropods -s` 实际上只会给予亡灵杀手魔咒，因为节肢杀手与亡灵杀手冲突。
- `-u`：只在已有魔咒等级低于 `<数值提供器>` 计算出的等级的情况下给予魔咒，也就是说，加上此选项之后，此魔咒修改只能升级魔咒，不能降级魔咒。
- 如果同时设置多个选项，可以像这样写：`-sc`。连字符后的各字符的顺序是随意的，但不可重复，例如 `-sc` 和 `-cs` 等价，但 `-scs` 是无效的。

## 示例

- `enchant(sharpness 5)`：给予等级为 5 的锋利。
- `enchant(density 3..5)`：给予等级为 3 到 5 之间的随机值的致密。
- `enchant(fortune upgrade 1)`：将时运升级一级。
- `enchant(fortune upgrade 1 -c)`：将时运升级一级，但不超过最高的合理等级。
- `enchant(fortune 2 -u)`：给予等级为 2 的时运，但如果已有时运魔咒不低于 2 级，则不作修改。
- `enchant(any_supported random_possible)`：给予随机的一个受支持的附魔，等级为合理范围内的随机等级。
- `enchant(all_supported max_reasonable)`：给予所有受支持的魔咒，等级为各魔咒的最高合理等级。
- `enchant(all_supported max_reasonable, !all of #curse)`：给予所有受支持的魔咒，等级为各魔咒的最高合理等级，但要移除所有的诅咒魔咒。
- `enchant(all max_possible)`：给予所有可能的魔咒，且等级为可能的最高等级，即为 255 级。
- `enchant(#curse)`：给予随机的一个诅咒魔咒。
- `enchant(any of #curse)`：给予随机的一个诅咒魔咒，和前一个例子等价。
- `enchant(all of #curse)`：给予所有的诅咒魔咒。
- `enchant(!all)`：移除所有魔咒。
- `enchant(smite 2, bane_of_arthropods 3)`：给予 2 级的亡灵杀手和 3 级的节肢杀手魔咒。
- `enchant(smite 2, bane_of_arthropods 3 -s)`：只会给予 2 级的亡灵杀手魔咒，因为节肢杀手与亡灵杀手冲突。
- `enchant(smite|bane_of_arthropods -s)`：只会给予亡灵杀手魔咒，因为节肢杀手与亡灵杀手冲突。
- `enchant(!punch)`：移除冲击魔咒。
- `enchant(!all of #curse)`：移除所有的诅咒魔咒。
- `enchant(natual 30)`：给予相当于有 30 个书架的附魔台给予的魔咒。
- `enchant(natual 25..30)`：给予相当于有 25~30 个（随机数）书架的附魔台给予的魔咒。

## 数据结构

- `type`：此时为 `"enhanced_commands:enchant"`。
- `modifications`：列表。
    - 复合标签。
        - `type`：ID，魔咒修改的类型，默认命名空间为 `enhanced_commands`，以下均省略命名空间。
        - （当 `type` 为 `add` 或 `remove`：）
        - `enchantment`：魔咒修改目标。字符串或复合标签。若为字符串，则为魔咒的 ID。若为复合标签，则有以下字段：
            - `type`：字符串（无命名空间），支持的值是 `single`、`tag` 或 `special`。
            - （当 `type` 为 `single`：）
            - `enchantment`：魔咒 ID。
            - （当 `type` 为 `tag`：）
            - `tag`：魔咒标签 ID（带 `#` 号前缀），或由魔咒 ID 组成的列表。
            - `all`：布尔值，可选，默认为 `false`。若为 `true`，表示这个标签的所有魔咒，而非任选一个魔咒。
            - （当 `type` 为 `special`：）
            - `value`：字符串，支持的值为：`any`、`all`、`any_supported`、`all_supported`、`any_present`、`all_present`。
        - （当 `type` 为 `add`：）
        - `level`：浮点数，或复合标签。若为复合标签，则有以下字段：
            - `type`：字符串（无命名空间），支持的值是 `basic`、`upgrade`、`special`。
            - （若 `type` 为 `basic` 或 `upgrade`，则有以下字段：）
            - `value`：数值提供器。
            - （若 `type` 为 `special`，则有以下字段：）
            - `value`：字符串，支持的值为：`random_reasonable`、`random_possible`、`max_reasonable`、`max_possible`、`upgrade_randomly`、`degrade_randomly`。
        - （当 `type` 为 `natural`：）
        - `level`：数值提供器。