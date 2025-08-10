# `gamemode`: Tests the player's game mode

This [entity predicate](../en.md) is used to test player's game mode. Same as vanilla, if an entity selector uses this predicate, the whole entity selector only selects players.

## Syntax

- `[gamemode=<game mode>]`: Selects players in the specified game mode.
- `[gamemode=!<game mode>]`: Selects players not in the specified game mode.
- `[gamemode=<game mode 1>|<game mode 2>...]`: Selects players in one of the specified game modes.
- `[gamemode=!<game mode 1>|<game mode 2>...]`: Selects players not in any of the specified game modes.

The game modes can use full names (such as `creative`), abbreviations (such as `c`) and numbers (such as `1`). Multiple game modes are allowed, but duplication is not allowed (even if written in different forms).

The argument name `gamemode` can be abbreviated to `m`; both are equivalent.

> Note: The option name of this predicate in the entity selector in commands is `gamemode`, without underscore, but the internal entity predicate type id is `enhanced_commands:game_mode`, with underscore.

## Examples

- `[gamemode=a]`: Selects players in Adventure Mode.
- `[gamemode=!c]`: Selects players not in Creative Mode.
- `@a[m=c|a]`: Selects players in Creative Mode or Adventure Mode.
- `@a[m=!c|s]`: Selects players in neither Creative Mode nor Survival Mode.
- `@a[m=c|1]`: Invalid, as `c` and `1` are duplicate.
- `@e[m=!spectator|c]`: Selects alive players neither in Spectator Mode nor in Creative Mode. Even if `@e` is used, it only selects players.
- `@a[m=!spectator|3]`: Invalid, as `spectator` and `3` are duplicate.

## Data structure

- `type`: Currently `"enhanced_commands:game_mode"`.
- `game_mode`: String or list. Name of the game mode.
    - String, name of the game mode (if `game_mode` is a list).
- `inverted`: Boolean, optional, by default `false`.