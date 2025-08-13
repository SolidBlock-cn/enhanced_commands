# `/if|unless rand`

The [command](../../en.md) is used to execute or not to execute the command under a certain probability.

## Syntax

- `/if|unless rand <probability> ...`

## Parameters

### `<probability>`

Floating-point number. The probability of the condition.

## Examples

- `/if rand 0.1 summon creeper`
- `/unless rand 0.1 setblock ~~~ lava`