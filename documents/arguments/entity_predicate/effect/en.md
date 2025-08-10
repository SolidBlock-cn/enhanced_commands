# `effect`: Tests the entity's status effects

This [entity predicate](../en.md) can be used to test status effects of entities. Can specify one single or multiple status effects.

## Syntax

- `[effect=<status effect>]`: Passes when the entity has this status effect. The status effect here is its id, such as `absorption`.
- `[effect=!<status effect>]`: Passes when the entity does not have this status effect.
- `[effect={<status effect 1>=<boolean 1>, <status effect 2>=<boolean 2>, ...]`: Passes when the various status effects of the entity match the specified boolean. The boolean is `true` and `false` and can be inverted: `!true` is equal to `false`, `!false` is equal to `true`.

## Examples

- `@a[effect=night_vision]`: All players with night vision effect.
- `@a[effect=strength, effect=speed]`: All player s with strength and speed effect.
- `@a[effect={strength=true, effect=true}]`: Equivalent to the previous example.
- `@a[effect=!fire_resistance]`: All players without fire resistance effect.
- `@a[effect={fire_resistance=false}]`: Equivalent to the previous example.
- `@a[effect={fire_resistance=!true}]`: Equivalent to the previous example.
- `@a[effect=!blindness, effect=night_vision]`: All players without blindness and with night vision.
- `@a[effect={blindness=false, night_vision=true}]`: Equivalent to the previous example.

## Data structure

When specifying a single effect directly:

- `type`: Currently `"enhanced_commands:effect"`.
- `effect`: String, the effect's id.
- `inverted`: Boolean, optional, false by default.

When using a map of effect and boolean:

- `type`: Currently `"enhanced_commands:effects"`.
- `effects`: List.
    - Element of the list.
        - `effect`: String, the effect's id.
        - `data`: Map. Same as the data structure of the effect data in the loot table predicate.
            - `amplifier`: Integer range. Optional.
            - `duration`: Integer range. Optional.
            - `ambient`: Boolean. Optional.
            - `visible`: Boolean. Optional.
        - `expected`: Boolean. True by default.