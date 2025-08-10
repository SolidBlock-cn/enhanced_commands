# Entity selector

An **entity selector**, or also known as **target selector**, is an [argument type](../en.md) used to select entities matching the specified conditions. Entity selectors already exist in vanilla Minecraft, and the mod extends the functionalities of entity selectors.

## Improvements compared to vanilla Minecraft

### More entity selector types

We know that only the following selectors are provided in vanilla:

- `@p`: The nearest player.
- `@r`: A random player.
- `@a`: All players.
- `@e`: All entities, including players, but not including dead entities.
- `@n`: The nearest entity, not including dead entities.
- `@s`: The executor of the command.

Besides the several entity selectors provided in vanilla, the mod provides some extra entity selectors, which can be used very conveniently.

- `@0`: No entities, which means selecting no entities. The selector is usually used on specific occasions where no entities need to be selected.
- `@E`: All entities, including dead entities. In vanilla, `@a` can select dead players, while `@e` only selects entities not dead. The mod's `@E` can select dead entities.
- `@n`: The nearest entity (removed since 1.21 as vanilla already supported it).
- `@R`: A random entity.
- `@f`: The furthest entity.
- `@nn`: The nearest entity except players.
- `@pn`: Equivalent to `@nn`.
- `@en`: All entities expect players.
- `@fn`: The furthest entity except players.
- `@rn`: A random entity except players.
- `@Rn`: Equivalent to `@rn`.
- `@pets`: All entities tamed by the current entity, equivalent to `@e[owner=@s]` in effect.
- `@owner`: The owner of the current entity. For example, if the executor of the command is a tamed wolf or horse, `@owner` will select the player taming the wolf or horse. Similar to `/execute on owner`.
- `@vehicle`: The entity that the current entity is riding. For example, if the executor of the command sits in a boat, `@vehicle` will select the boat. Similar to `/execute on vehicle`.
- `@passengers`: All entities riding the current entity. For example, if the executor of the command is a boat, `@passengers` will select entities sitting on the boat, similar to `/execute on passengers`.
- `@leasher`: The entity leashing the current entity. For example, if the executor of the command is a villager leashed by a player, `@leasher` will select the player leashing the entity, similar to `/execute on leasher`.
- `@origin`: The origin of the entity. For example, if the executor of the command is a snowball, `@origin` will select the player that threw the snowball. Similar to `/execute on origin`.
- `@attacker`: The attacker of the current entity. Similar to `/execute on attacker`.
- `@target`: The target of the current entity. Similar to `/execute on targets`.
- `@controlling_vehicle`: Selects the entity that the current entity is riding and controlling. For example, if a player and a villager sit on the same boat, as only players control boats, when the executor of the command is a player, the selector will select the boat, while when the executor is the village, the selector can’t select the boat.
- `@controller`: Selects the entity riding and controlling the current entity. For example, if a player and a villager sit on the same boat, when the executor of the command is the boat, the selector will select the player instead of the villager.

In the selectors above, entity selector type `@pets` and those after it allow to specify the relevant entities of the specified entity with the option "`of`", instead of relevant entities of the current entity. For example, `@pets[of=Steve]` can select all entities that the player Steve tames (equals to `@e[owner=Steve]` in effect). `@passengers[of=@n[type=minecart]]` can select passengers on the nearest minecart. See [the "`of`" option](#of).

### Specifying `type` for `@p` and `@r`

Vanilla `@p` and `@r` only select players. To select the nearest entity of other types, you must use `@e[type=..., sort=nearest/furthest]`, which is too inconvenient! (Selector `@n` is added since 1.21.) But it's good that the mod modified it: if you use `@p` and `@r`, you can also select entities of other types. For example:

- `@p[type=creeper]`: Selects the nearest creeper.
- `@p[type=!creeper]`: Selects the nearest entity that is not a creeper.
- `@r[type=cow]`: Selects a random cow.
- `@r[type=!player]`: Selects a random entity that is not a player.

### Specifying multiple types

The vanilla `type` parameter can only specify one type or tag. The mod modified it so the selector can specify entities of multiple types or tags, but duplications aren’t allowed. For example:

- `@p[type=cat|wolf]`: The nearest cat or wolf.
- `@e[type=player|iron_golem]`: All players or iron golems.
- `@e[type=zombie|zombie]`: Invalid, due to duplicate values.

### Simplified parameter aliases

If you remember playing old versions, you may know that the syntax of entity selectors was quite simple. For example, `c` to limit the count (now we use `limit`), `r` and `rm` to limit the furthest and nearest distance (now we use `range`). The syntax now is too complicated! The mod brings back old syntax back.

- `c` is equivalent to `limit`. You can’t use both `c` and `limit`.
- `r` and `rm` means the furthest and nearest distance, equivalent to `distance=<rm>..<r>`. If `r` and `rm` are specified, `distance` cannot be used.
- `m` is equivalent to `gamemode`. You can’t use both `m` and `gamemode`.

For example:

- `@p[c=5]`: Nearest 5 players.
    - Equivalent to `@p[limit=5]`.
- `@e[r=5]`: All entities within the 5-block distance.
    - Equivalent to  `@e[distance=..5]`.
- `@e[r=5,rm=3]`: All entities within the 5-block distance but at least 3-block distance.
    - Equivalent to `@e[distance=3..5]`.
- `@e[c=5,limit=6]`: Invalid, as `c` and `limit` are duplicate.
- `@s[c=5]`: Invalid, as `@s` cannot specify entity amounts.
- `@e[r=3,rm=5]`: Invalid, because the furthest distance can’t be lower than the nearest distance.

> Note: The mod doesn’t support `rx`, `rxm`, `ry` and `rym` yet.

### Simplified gamemode aliases and using multiple gamemodes.

In versions before the Ocean Update, gamemodes can be expressed quite simply as `s`, `c`, `a`, or`sp`, or integers between 0 and 3. But we must use full names now, which is unreasonable. Therefore, the mod brings back the simplified aliases of gamemodes.

For example, the following examples mean selecting players in Creative Mode (`m` and `gamemode` are equivalent, see [#Simplified parameter aliases](#simplified-parameter-aliases)):

- `@a[gamemode=creative]` (vanilla usage)
- `@a[m=creative]`
- `@a[gamemode=c]`
- `@a[m=c]`
- `@a[gamemode=1]`
- `@a[m=1]`

Besides, the mod supports selecting multiple gamemodes (duplications not allowed). For example:

- `@a[m=c|a]`: Selects players in Creative Mode or Adventure Mode.
- `@a[m=!c|s]`: Selects players in neither Creative Mode nor Survival Mode.
- `@a[m=c|1]`: Invalid, as `c` and `1` are duplicate.

### Providing suggestions for more occasions

When typing entity selectors in vanilla, where some contents such as advancements and score terms are needed, no suggestions are provided, causing some inconvenience when we type commands. The mod improves it, allowing it to display suggestions in the following situations:

- Advancement ids, such as `@e[advancements={ <- [here]`
- Criterion names of advancements (compared to vanilla, quoted strings are allowed here, and when the string contains some special characters, suggestions are provided with quotation marks). For example: `@e[advancements={<id> = <- [here]`
- Score names, such as: `@a[scores={ <- [here]`
- Loot predicate ids, such as: `@r[predicate= <- [here]`

### More conditions to be inverted

In vanilla, some options or entity selectors can be inverted with a `!` mark, such as `@e[type=!player]`, which means all entities that aren’t players. The mod makes the feature support more occasions, including:

- Inverting when selecting experience levels, such as:
    - `@a[level=!2]`: Selects players whose experience levels aren’t 2.
    - `@a[level=!2..3]`: Selects players whose experience levels aren’t between 2 and 3.
  > Note: As long as experience levels are specified, even if the condition is inverted, the selected can’t select any entities other than players.
- Inverting when selecting scores, such as:
    - `@e[scores={a=!2}]`: Selects entities whose value in score "a" is not 2.
    - `@e[scores={a=!3, b=!5..6}]`: Selects entities whose value in score "a" is not 3 and value in score "b" is not between 5 and 6.
  > Note: Even if inverted, when the score objective doesn’t exist, or the entity doesn’t have a score in this objective, the entity can’t be selected. For example, `@e[scores={a=!2}]` to be described most accurately, is when the score "a" exists, selecting players who have a value on this score objective, which does not equal to 2.

### Specifying loot predicate JSONs

The loot predicate is a strong functionality of Minecraft, which supports some complicated judgements on conditions. However, loot predicates are defined in datapacks, and the storage is slightly complicated. With this mod, you can specify a loot predicate directly in the command without a datapack.

Compared to vanilla, for convenience, the JSON parsing here is lenient, supporting unquoted string. It is also supported to add a `!` after the `=` to invert the predicate. You can also invert by using `{condition: inverted, term: {...}}`.

Where you type loot predicates, directly type the JSON, and the command will be parsed directly during compilation, quite convenient. For example:

- `@e[predicate={condition: entity_properties, entity: this, predicate: {flags: {is_sneaking: true}}}]`: Sneaking entities.
- `@e[predicate={condition: location_check, predicate: {biome: badlands}}]`: Entities in the badlands biome.

> It's advised to avoid using direct loot predicates if other selector parameters can achieve the same effect.

### Better error tips

When you type something wrong in the entity selector, including wrong parameters, or some parameters that don’t exist or are duplicate, the mod provides more detailed error tips, such as noticing some parameter is duplicate, or not supported in the specific entity selector.

## Extra parameters

On the basis of vanilla entity selector, the mod adds some extra parameters to support more functionalities. The parameters are also available in [entity predicates](../entity_predicate/en.md).

### air

Tests the air value of the entity. Accepts int ranges or the keyword `max` which means the max air value. Can be inverted.

- `@e[air=5..]`: Selects entities whose air values aren’t less than 5.
- `@a[air=!..0]`: Selects entities whose air values are greater than 0.

See [the `/air` command](/documents/commands/air/en.md).

### alternatives

Specifying multiple entity predicates. The entity only needs to pass one of the predicate tests. Values of the parameters are wrapped in a square bracket, with multiple entity predicates inside; multiple entity predicates are separated with comma. The parameter be inverted.

- `@e[alternatives=[[type=cow], [type=sheep]]]`: Selects all cows and sheep.
- `@a[alternatives=[Player1, Player2]]`: Selects Player1 and Player2.
- `@e[type=sheep, alternatives=[[tag=a], [tag=b]]]`: Within all sheep, select those with tag "a" or tag "b".
- `@e[type=sheep, alternatives=![[tag=a], [tag=b]]]`: Within all sheep, select those with neither tag "a" nor "b".

### baby

Tests whether the entity is a baby. If the entity type doesn’t support baby entities, entities will be seen as non-babies.

- `@e[baby=true]`: Selects all baby entities.
- `@e[baby=false]`: Selects all entities who aren’t babies (including entities who can’t be babies).

The parameter can be inverted. For example, `@e[baby=!true]` is equivalent to `@e[baby=false]`.

### exhaustion

Tests the exhaustion value of the entity. Accepts float-point number ranges. Can be inverted. Similar to [`food`](#food), only players can be selected when this parameter is added.

- `@a[exhaustion=0.5..]`: Selects players whose exhaustion value is at least 0.5.

See [the `/food` command](/documents/commands/food/en.md)》

### fire

Tests the remaining on-fire time (ticks to the file extinguish naturally). Can be inverted.

- `@e[fire=1..]`: Selects entities whose remaining on-fire time is at least 1 (which mean currently on fire).
- `@e[fire=20..]`: Selects entities whose remaining on-fire time is at least 20.

### food

Tests the food value of entities. Accepts integer ranges. Can be inverted.

Only players have food values, so adding this parameter will only select players.

- `@e[food=20]`: Select players whose food value is 20 (although `@e` is used, only players can be selected).
- `@a[food=10..15]`: Select players whose food value is between 10 and 15.
- `@a[food=!10]`: Select players whose food value is not 10.

See [the `/food` command](/documents/commands/food/en.md).

### health

Tests the health value of entities. Accepts a range, or the keyword `max`. The parameter can be inverted. This parameter only selects health values of living entities. Entities such as minecarts and dropped items also have their special health values but can’t be used to select by the entity selector.

- `@e[health=20]`: Selects living entities whose health value is 20.
- `@e[health=!20]`: Selects living entities whose health value is not 20.
- `@e[health=max]`: Selects living entities whose health value is the max value.
- `@e[health=10..]`: Selects living entities whose health value is not less than 10.

See [the `/health` command](/documents/commands/health/en.md).

### is

Specifies an [entity predicate](../entity_predicate) and only passes when the entity also matches the predicate. This means filtering entities selected. The option can be inverted, which is identical to [`not`](#not) in effect.

- `@p[is=@s]`: The nearest entity, and meanwhile the entity should be the executor of the command.
- `@a[is=@e[type=cow]]`: All players that are cows. This entity selector is valid but obviously cannot select any entity.
- `@e[is=!@s]`: All entities except the command executor. Identical to `@e[not=@s]` in effect.

### not

Specifies an [entity predicate](../entity_predicate) and only passes when the entity does not match the predicate. This means excluding some entities selected. The option can be inverted, which is identical to [`is`](#is).

- `@e[not=@s]`: All entities except the command executor.
- `@e[not=[type=cow]]`: All entities except cows. Identical to `@e[type=!cow]` in effect.
- `@e[not=@s, not=@pets]`: All entities except the command executor and pets of the command executor. Identical to `@e[alternatives=![@s, @pets]]` in effect.
- `@e[type=cow, not=!@s]`: All cows that are the command executor. Identical to `@e[type=cow, is=@s]`, `@s[type=cow]`.

### of

Specifies an entity selector, used for some specific entity selector types such as `@pets`, `@vehicle`, meaning selecting relevant entities of the specified entities, instead of the relevant entities of the command executor. This option cannot be used once. See [#more entity selector types](#more-entity-selector-types).

In other entity selectors, the option "`of`" can be used but has no effect.

- `@pets`: All entities that the command executor tames. Identical to `@e[owner=@s]`.
- `@pets[of=Alex]`: All entities that the player Alex tames. Identical to `@e[owner=Alex]`.
- `@vehicle`: Entities that the command executor is riding.
- `@vehicle[of=@a]`: Entities that any player is riding.
- `@passengers`: All passengers of the command executor.
- `@passengers[passengers=@e[type=horse]]`: All passengers of all horses.

> Note: `@pets` cannot be used along with [the `owner` option](#owner), or parsing error will be caused. For example, to select all pets of Steve, you may use `@pets[of=Steve]` or `@e[owner=Steve]`, but cannot use `@pets[owner=Steve]`.

### on_fire

Tests whether the entity is on fire. Accepts a boolean value. If the entity is immune to fire, they’re seen as not on fire.

- `@e[on_fire=true]`: Selects entities on fire (and not immune to fire).
- `@e[on_fire=false]`: Selects entities not on fire (or immune to fire).

This parameter can be inverted. For example, `@e[on_fire=true]` and `@e[on_fire=!false]` are equivalent.

### owner

Filter by entities' owners. The value can be an [entity predicate](../entity_predicate/en.md) or no value.

This option can be inverted. When no value is specified, it selects untamed entities, including untamable entities.

- `@e[owner=]`: Selects all entities not tamed, including untamable entities.
- `@e[owner=!]`: Selects all tamed entities, regardless of the owner (can be players no in the current world).

If a selector is provided as a value, then no matter inverted or not, entities can’t be selected when they’re untamable, the owner doesn’t exist, or not in this world.

- `@e[owner=<实体谓词>]`: Selects all tamed entities whose owners match the specified entity predicate.
- `@e[owner=!<实体谓词>]`: Selects all tamed entities whose owners don’t match the specified entity predicate.

For example:

- `@e[owner=Alice]`: Selects all entities tamed by player Alice. It fails when Alice is not in the world.
- `@e[owner=!@p]`: Selects all entities tamed by any player who is not the nearest player to the execution position.
- `@e[owner=@s]`: Selects all entities tamed by the executor of the command, similar to `@pets`.

### pose

Selects entities in the specified pose.

- `@e[pose=crouching]`: Selects all crouching entities.
- `@a[pose=!swimming]`: Selects all entities not swimming.

### region

Selects entities within a specified [region](/documents/arguments/region/en.md). Relative coordinates are local coordinates are supported, which are calculated according to the position and rotation of the command source (instead of the selected entities' position and rotation).

- `@e[region=sphere(5)]`: Selects entities within a sphere range, whose center is the command source's position, and radius is 5.

### saturation

Tests the saturation of entity. Accepts float-point number ranges. Can be inverted. Similar to [`food`](#food), only players can be selected when this parameter is added.

- `@e[saturation=5..]`: Selects players whose saturation is at least 5.
- `@e[saturation=!0]`: Selects players whose saturation is not 0.

See [the `/food` command](/documents/commands/food/en.md).

### sneaking

Tests whether the entity is sneaking. Players flying and descending will also be selected. This option is different from the loot predicate.

- `@e[sneaking=true]`: Selects sneaking entities.
- `@e[sneaking=false]`: Selects entities not sneaking.

The parameter can be inverted but usually unnecessary. For example, `@e[sneaking=!true]` is equivalent to `@e[sneaking=false]`.

### sprinting

Tests whether the entity is sprinting. The parameter can be inverted.

- `@e[sprinting=true]`: Selects sprinting entities.
- `@a[sprinting=false]`: Selects entities not sprinting.

### swimming

Tests whether the entity is swimming. The parameter can be inverted.

- `@e[swimming=true]`: Selects swimming entities.
- `@a[swimming=false]`: Selects entities not swimming.

## Data structure

Entity selectors support serialization and deserialization through this mod. Some mods may add other properties or entity predicates into entity selectors. Contents added by other mods may not be serialized by this mod. Entity selectors specified through command arguments can usually be serialized normally, but entity selectors directly created by codes may not be serialized fully for all data,

* `limit`: Integer, optional, by default 2<sup>31</sup> - 1.
* `player_only`: Boolean, optional, by default `false`.
* `local_world_only`: Boolean, optional, by default `false`.
* `predicates`: List, optional. Usually not including entity predicates of special types.
    * [Entity predicate](../entity_predicate/en.md)
* `distance`: Double floating-point, or a range defined by a map, optional.
* `position_offset`: Map, optional.
* `dx`: Double floating-point, optional.
* `dy`: Double floating-point, optional.
* `dz`: Double floating-point, optional.
* `sort`: String, optional, by default `arbitrary`. The sorting that the entity selector uses.
* `sender_only`: Boolean, optional, by default `false`.
* `player_name`: String. Optional.
* `uuid`：UUID (usually a list or array), optional.
* `entity_type`: String, specify an entity type, optional.
* `uses_at`: Boolean, optional, by default `false`, which means whether "@" is used.
* `collector`: String, optional. The special collector of entity selector.
* `collector_of`: Entity selector, optional. Corresponds to the `of` argument in the entity selector arguments.