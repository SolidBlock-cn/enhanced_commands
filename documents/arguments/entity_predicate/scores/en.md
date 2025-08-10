# `scores`: Tests entity's scoreboard score

This [entity predicate](../en.md) tests an entity's scoreboard score. Similar to vanilla `scores` argument, it takes a map (key-value pair) of "objective name - range", and this mod supports inversion to the number range.

When the specified score objective does not exist, or the entity has no score on this objective, the test always fails, regardless of whether inverted.

## Syntax

- `[scores={<score objective name> = <range>}]`: Selects entities whose score on this objective is in the specified range.
- `[scores={<score objective name> =! <range>}]`: Selects entities who has score on this objective and the score is not in the specified range.

## Examples

Here only non-vanilla examples are shown.

- `@a[scores={a=!2}]`: Selects players who have score on the objective "a" and the value of score "a" is not 2.
- `@e[scores={a=!3, b=!5..6}]`: Selects alive entities who have scores on scoreboard objectives both "a" and "b", and the value of score "a" is not 3, value of score "b" is not between 5 and 6.

## Data structure

- `type`: `enhanced_commands:scores`.
- `scores`: List.
    - Element in the list. Map.
        - `name`: String.
        - `score`: Integer range.
        - `inverted`: Boolean, optional, by default `false`.