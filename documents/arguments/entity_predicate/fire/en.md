# `fire`: Tests the entity's on-fire time

This [entity predicate](../en.md) tests the remaining on-fire time (ticks to the file extinguish naturally). It can be inverted.

## Examples

- `@e[fire=1..]`: Selects entities whose remaining on-fire time is at least 1 game tick (which mean currently on fire).
- `@e[fire=20..]`: Selects entities whose remaining on-fire time is at least 20 game ticks (1 second).
- `@a[fire=!..30]`: Selects entities whose remaining on-fire times is not within 30 game ticks (1.5 seconds).

## Data structure

- `type`: Currently `"enhanced_commands:fire"`.
- `time`: Integer range.
- `inverted`: Boolean, optional, by default `false`.