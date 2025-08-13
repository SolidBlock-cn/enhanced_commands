# `/debug:permissionlevel`

The [command](../en.md) can be used to get the current permission level, and execute a command with a specified permission level.

## Syntax

- `/debug:permissionlevel`: Get the current permission level.
- `/debug:permissionlevel <level> ...`: Execute a command with a specified permission level. It usually does not work, because the parsing of the command is usually not affected by this, but some situations may be affected, such as not able to use entity selectors. This command is only used to debug.

## Parameters

### `<level>`

Integer.