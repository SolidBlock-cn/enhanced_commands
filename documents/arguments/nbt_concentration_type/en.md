# NBT concentration type: converting multiple NBTs to one

**NBT concentration type** is an [argument type](../en.md), specifying how multiple NBT data are converted into one. Similar to [concentration type](../concentration_type/en.md), but can handle NBT data.

NBT concentration types support the following values:

- `first`: The first value.
- `last`: The last value.
- `min`: The minimum value.
- `max`: The maximum value.
- `list`: Form a list containing all NBT data. If the data has mixed different types, exceptions will be thrown.
- `random`: Get a random value among the NBT data.
- `all`: When all the data contains only one, returns the data. If there are multiple, exceptions will be thrown, unless some commands handle this specially. For example, the [`/nbt` command](../../commands/nbt/en.md) can display multiple data directly.

When the concentrated data has zero values, unless the concentration type is `list`, exceptions will be thrown. The `list` concentration type concentrates zero data into one empty list.

Here are some examples of NBT concentration type being used in commands:

- `/nbt get entity @a Pos[1] max`: Get the maximum y-axis coordinate of all players.
- `/nbt get entity @e[type=sheep] Color`: Get the wool color of all sheep. The concentration type here is using the default value `all`. Wool colors of each entity will be displayed in the command feedback.
- `/nbt get entity @p[c=5] Health list`: Form a list of health values of the nearest five players, and get the list.

Here are some examples of concentration result of NBT concentration types:

* For `1b`, `3f`, `2s`:
    * `first`: `1b`
    * `last`: `2s`
    * `min`: `1b`
    * `max`: `3f`
    * `list`: error
* For `[1]`, `5`, `"x"`:
    * `first`: `[1]`
    * `last`: `"x"`
    * `min`: error
    * `max`: error
    * `list`: error
* For `"iron"`, `"gold"`, `"diamond"`, `"netherite"`:
    * `first`: `iron`
    * `last`: `netherite`
    * `min`: `diamond`
    * `max`: `netherite`
    * `list`: `["iron", "gold", "diamond", "netherite"]`
* For no data:
    * `first`: error
    * `last`: error
    * `min`: error
    * `max`: error
    * `list`: `[]`