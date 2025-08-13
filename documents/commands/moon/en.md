# `/moon`: View and set moon phase

The [command](../en.md) can be used to view and set moon phase, and execute commands under a condition related to moon phase. An alias of this command is `/jadeplate`.

## Syntax

- `/moon`: Equivalent to `/moon get`.
- `/moon get`: Query the current moon phase. Returns the numeric id of the current moon phase.
- `/moon set <moonphase>`: Set the moon phase to a specified value.
- `/moon rotate [steps]`: Rotate the current moon phase. For example, steps being 1 sets to the next moon phase, and being -1 sets to the previous moon phase.
- `/moon if|unless phase <moon phase> ...`: If the current moon phase is or is not the specified moon phase, then execute the command.
- `/moon if|unless size <size> ...`: If the current moon is or is not in the specified range, then execute the command.

## Parameters

### `<moon phase>`

The moon phase supports the following values

| numeric id | id                | size |
|------------|-------------------|------|
| 0          | `full_moon`       | 1    |
| 1          | `waning_gibbous`  | 0.75 |
| 2          | `third_quarter`   | 0.5  |
| 3          | `waning_crescent` | 0.25 |
| 4          | `new_moon`        | 0    |
| 5          | `waxing_crescent` | 0.25 |
| 6          | `first_quarter`   | 0.5  |
| 7          | `waxing_gibbous`  | 0.75 |

### `[steps]`

Integer. Range not limited. If not specified, by default 1. Each integer is equivalent to all integers congruent to it modulo 8.

For example, if the current moon phase is first quarter (id = 6), and rotation steps 1, 9, 7... or -7, -15, -23... set to waxing gibbous (id = 7), rotation steps 2, 10, 18... or -6, -14, -22... sets to full oon (id = 0).

> ℹ️Note: The essence of setting moon phase is setting game time.

### `<size>`

Floating-point range.

## Example

- `/moon get`: Get the current moon phase.
- `/moon set new_moon`: Set the current moon phase to full moon.
- `/moon rotate 3`: Rotate the current moon phase 3 forward, for example, if it is full moon (id = 0), it will be set to waning crescent (id = 3), if it is waxing crescent (id = 5) it will be set to third quarter (id = 2).
- `/moon rotate -2`: Rotate the current moon phase 2 backward, for example, if it is full moon (id = 0), it will be set to first quarter (id = 6), if it is waxing gibbous (id = 7) it will be set to waxing crescent (id = 5).
- `/moon if phase full_moon summon zombie`: If it is full moon, summon a zombie.
- `/moon unless phase full_moon give @a glowstone`: If it is not full moon, give all players glowstone.
- `/moon if size 0.75.. summon slime`: If the size of the moon phase is not lower than 0.75, summon a slime.
- `/moon unless size 0.25..0.75 summon skeleton`: If the size of the moon phase is not between 0.25 and 0.75 (which means if it is a new moon or a full moon), summon a skeleton.