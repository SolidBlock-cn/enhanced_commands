# `/testfor biome`

This [command](../../en.md) can be used to test on biome.

## Syntax

- `/testfor biome [pos]`: Test the biome of the specified [pos](/documents/arguments/pos/en.md). If the pos is not specified, the pos of command execution will be used.
- `/testfor biome <pos> <biome>`: Test whether the specified pos matches the specified biome. Supports multiple values. Supports tags.

## Examples

- `/testfor biome`
- `/testfor biome ~~5~`
- `/testfor biome ~~~ plains`
- `/testfor biome ~~~ forest|flower_forest`
- `/testfor biome ~ 0 ~ !desert|badlands`
- `/testfor biome ~~~ !!allows_surface_slime_spawns`