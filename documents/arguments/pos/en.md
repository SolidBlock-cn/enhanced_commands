# Pos

**Pos** describes a coordinate of the in a world. The position can be an absolute position that specified all coordinate axes, or also a relative coordinate related to the command source. The argument is similar to Minecraft's vanilla pos argument, but can omit space.

## Syntax

- absolute coordinate and relative coordinate: `(<absolute x>|~[relative x])(<absolute y 值>|~[relative y])(<absolute z>|~[relative z])`
- local coordinate: `^[local x]^[local y]^[local z]`

On the basis of not causing parsing exception, the space between the parts of the coordinate is optional. But, space cannot be added between `~` (tilde) or `^` (insertion mark) and the number following.

Absolute coordinates/relative coordinates cannot be used in mixture with local coordinates. For example, if one part uses local coordinate, then the other two parts also should be local coordinates.

When the specified coordinate can only be an integer, the value of the absolute coordinate must be an integer, but relative coordinates and local coordinates may be decimals.

In some cases, the integer value using absolute positions are aligned to the center, but not aligned if `.0` is added. For example, in some parameters of the command, `1 2 3` equals to `1.5 2 3.5`, instead of `1.0 2 3.0`.

## Examples

- `0 20 40`: The absolute coordinate (0, 20, 40).
- `0 ~ 40`: Relative coordinate where, x is 0, y is the y of the command source, z is 40. The space between `0` and `~` can be omitted, but the space between `~` and `40` is not omittable.
- `~ ~20 ~`: Relative coordinate which is the coordinate whose y added 20 from the command source position. Can be abbreviated to `~~20~`.
- `~10~20~30`: Relative coordinate, the coordinate whose x, y, z added 10, 20, 30 respectively from the command source.
- `^ ^ ^5`: Local coordinate, the coordinate moved 5 blocks from the command source in its direction. Can be abbreviated to `^^^5`.
- `1 2 ^3`: Invalid coordinate.
- `^ ^2 ~5`: Invalid coordinate.

> By default, when the player executes the command, the position and rotation of the command source is the player's position and rotation, which can be modified with the attribute subcommands of `/execute`, including independent commands separated in the mod.

## Data structure

- `type`: String. The coordinate's type supports the following values: `default_int`, `default_double`, `looking_pos`, `unknown`.

If `type` is `default_int`, it supports the following fields:

- `x`: Integer.
- `y`: Integer.
- `z`: Integer.
- `x_relative`: Boolean, optional, by default `false`.
- `y_relative`: Boolean, optional, by default `false`.
- `z_relative`: Boolean, optional, by default `false`.

If `type` is `default_double`, it supports the following fields:

- `x`: Double-precision floating-point.
- `y`: Double-precision floating-point.
- `z`: Double-precision floating-point.
- `x_relative`: Boolean, optional, by default `false`.
- `y_relative`: Boolean, optional, by default `false`.
- `z_relative`: Boolean, optional, by default `false`.

If `type` is `looking_pos`, it supports the following fields:

- `x`: Double-precision floating-point.
- `y`: Double-precision floating-point.
- `z`: Double-precision floating-point.