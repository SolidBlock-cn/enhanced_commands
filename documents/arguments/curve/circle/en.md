# `circle()`: draw an arbitrary circle

The circle or part of circle (circle arc) at the specified center with around the specified rotation axis vector. The circle is perpendicular to the rotation axis vector. This curve has the following basic properties:

- Radius: Double-precise, double precision floating point number.
- Center: The coordinate of the circle center.
- Pivot: The rotation axis of the circle, which is a vector. The plane of the circle is perpendicular to this vector.
- Angle range: The rotation range of the circle. By default, 0 to 2π, which is a full turn, to draw a complete circle.

The circle drawn with this curve can be any size and any angle. In comparison, the region [`hcyl()`](/documents/arguments/region/cylinder/en.md) can draw a complete circle rotating around the y-axis.

When drawing the circle, a basic radius vector will be determined according to the radius and pivot, following the following steps:

- Calculate the vector product of the pivot and the unit vector in the positive y-direction.
- If the vector product is not zero vector, normalize the vector product and multiply by the radius.
- If the vector product is zero vector (such as when the axis is a zero vector or parallel to y-axis):
    - If the axis is the zero vector, use the vector in the positive-x direction whose length is equal to the radius.
    - If the axis is the vector in the negative-y direction, use the radius in the negative-x direction whose length is equal to the radius.

After the basic radius vector, rotate it centered at the circle center around the specified pivot as the rotation axis, by the certain angle. The angle is the arbitrary value within the angle range, so as to form a circle or circle arc.

## Syntax

- `circle(<radius>, [center], pivot = [pivot], range = [range])`
- `circle(range = <radius>, center = [center], pivot = [pivot], range = [range])`

The parameter name of `range` and `center` can be omitted, but if omitted, they must be declared in order. When the parameter name is specified clearly, the order of the parameters can be exchanged, but cannot be duplicated.

## Parameters

### `<radius>`

Radius of the circle. Double precision floating point number, which can be an arbitrary number. If zero, draw a point. If negative number, draw a circle or circle arc normally.

### `[center]`

Coordinate, which can be a relative or local coordinate. If not specified, use the coordinate of the command source (such as the player's position).

### `[pivot]`

The rotation axis of the circle. Can be specified in the following ways:

- `<vector>`: Directly specify the axis vector with 3 double-precision floating-point numbers, such as `1 2 3`.
- `[length] <direction>`: The axis vector is the specified direction. The default value of `[length]` is 1, and usually is not needed to specified, because it does take effect as a rotation axis (unless its length is 0).
- `rotated <y> <x>`: Specify yaw and pitch in degrees (without units).
- `facing <pos>`: The vector from the command source to the specified coordinate.

When the rotation axis is not specified, use the unit vector in the positive-y direction.

### `[range]`

The angle range of the rotation. Format:

- `<start angle>..<end angle>`, the size of the two angles are not required, and can be compared correctly.
- `<end angle>`: Identical to `0..<end angle>`.

The format of angle is (all are double-precision floating-point numbers):

- `0`: When specifying a zero angle, the unit is not required.
- `<value>deg`: The angle in degrees. One full circle is 360 degrees.
- `<value>rad`: The angle in radians. One full circle is 2π.
- `<value>turn`: Specify the number of full circles. One full circle is 1turn.

## Examples

- `circle(5)`: The full circle whose center is the current coordinate, radius is 5 and the axis is the positive-y direction (which means on the horizontal plane).
- `circle(5, range = 0.5turn)`: The north-half circle arc.
- `circle(5, range = 0..0.5turn)`: Identical to the previous example.
- `circle(5, range = 0..180deg)`: Identical to the previous example.
- `circle(5, range = 0.25turn..0.75turn)`: The west-half circle arc.
- `circle(5, pivot = east)`: The full circle whose axis is the positive-x direction, which means it is on the y-z plane.
- `circle(5, pivot = 0 1 2)`: The circle rotating around vector (0, 1, 2). The plane where the circle lays is perpendicular to this vector.
- `circle(5, pivot = rotated 45 20)`: Circle in the rotation (45, 20).
- `circle(5, pivot = facing ^ ^ ^1)`: The circle facing as the position 1 block player's forward. The plane of the circle is perpendicular to the player's sight.
- `circle(5, center = ^^^5, pivot = facing ^^^5)`: The circle positioned as the position 5 block player's forward of sight, and its axis is the rotation of player's sight. The plane where the circle lays is perpendicular to the player's sight.
- `circle(5, ~ ~5 ~)`: THe circle whose center is the position at player's 5 block above, and axis is the positive-y direction (on the horizontal plane).
- `circle(radius = 5, center = ~ ~5 ~)`: Identical to the previous example.

## Data structure

- `type`: Currently `"enhanced_commands:circle"`.
- `radius`: Double-precision floating-point number.
- `center`: Coordinate.
- `pivot`: Coordinate.
- `min_angle`: Double-precision floating-point number. Optional. By default 0.
- `max_angle`: Double-precision floating-point number. Optional. By default 2π.