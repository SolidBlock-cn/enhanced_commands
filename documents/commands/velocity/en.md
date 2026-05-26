# `/velocity`: Modify entity's velocity

The [command](../en.md) is used to entity's velocity.

> Modifying players' velocity is an experimental feature. It may not work stably and sometimes may not work. In details:
> - In version 1.21.1, modifying player's speed does not work.
> - In version 1.21.4, modifying player's speed works, but the server may not accurately identify player's current speed. When the player is falling or triggered movement by an explosion, the speed can be identified by the server. But when the player is walking or flying by themselves, the server may not identify the velocity and it may be identified as a zero vector by the server.

## 语法

- `/velocity get [target] [聚合类型]`: Get entities' speed.
- `/velocity set <target> <vector>`: Set the entities' speed.
- `/velocity add <target> <vector>`: Add a vector to the entities' velocity.
- `/velocity subtract <target> <vector>`: Subtract a vector from the entities' velocity.
- `/velocity scale <target> <scale>`: Multiply a specified scale to the entities' velocity.
- `/velocity multiply <target> <vector>`: Multiply the components of the velocity of entities to the corresponding component of the specified vector. For example, if the entity's original velocity is $\vec a = (a_x, a_y, a_z)$ and the specified vector is $\vec b = (b_x, b_y, b_z)$, the entity's velocity will be modified to $(a_x b_x, a_y b_y, a_z b_z)$.
- `/velocity cross <target> <vector>`: Calculate the vector product of the specified entities and the specified vector, as the new vector of entities'. For example, if the entity's original velocity is $\vec a$, and the specified vector is $\vec b$, the entity's velocity will be modified to $\vec a \times \vec b$. (To calculate $\vec b \times \vec a$, please use $\vec a \times (-\vec b)$.)
- `/velocity setsize <target> <size>`: Modify the size of entities' velocity. If the size is positive, the direction does not change. If the size is negative, the size is inverted. If the entities' original speed is 0, nothing happens. For example, if the entity's speed is $\vec a$ ($\vec a \ne \vec 0$), specified size is $s$, the entity's velocity will be modified to $\dfrac {\vec a}{\left |\vec a\right |} \cdot s$.

## Parameters

### `<target>`

[Entity selector](/documents/arguments/entity_selector/en.md). The entities whose speed to modify.

### `<vector>`

The vector composed of three floating-point number.

> Currently, relative coordinates and local coordinates are supported but not suggested.

### `<scale>`/`<size>`

Double-precision floating-point number.

### `[concentration type]`

[Concentration type](/documents/arguments/concentration_type/en.md). If not specified, `average` is used.

## Examples

- `/velocity get @s`
- `/velocity get @a average`
- `/velocity get @e max`
- `/velocity set @s 0 1 0`
- `/velocity add @e 0 0.5 0`
- `/velocity subtract @e 0 -0.5 0`: Identical to the previous example.
- `/velocity scale @a 1.6`
- `/velocity multiply @a 1.6 1.6 1.6`: Identical to the previous example.
- `/velocity multiply @a 1 0 1`
- `/velocity cross @e[type=pig] 0 1 0`
- `/velocity setsize @a 0.5`