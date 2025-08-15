# `/testarg`

This [command](../en.md) is used to test the argument types of commands, and test the encoding and decoding of the objects.

## Syntax

- `/testarg block_function <block function> [string|nbt|json|string_test|nbt_test|json_test]`: [Block function](/documents/arguments/block_function/en.md).
    - `string`: Convert to string.
    - `nbt`: Encode into NBT.
    - `json`: Encode into JSON.
    - `string_test`: Convert to string, and parse according to the string, and checker whether the converted result is identical to the original string. Usually it should return `true`, but if it returns `false`, it means there is consistency within this conversion: strings converted (or NBT, JSON encoded) from Java objects are not correctly converted (or decoded) to the same Java object as original ones, which means bugs in this mod, or maybe there are some unsupported contents. This command is usually test the consistency during the object conversion. Same to the following `nbt_test` and `json_test`.
    - `nbt_test`: Encode to NBT, and decode to Java objects, and test whether the converted result is identical to the original one.
    - `json_test`: Encode to JSON, and decode to Java objects, and test whether the converted result is identical to the original one.
- `/testarg block_predicate <block predicate> [string|nbt|json|string_test|nbt_test|json_test]`: [Block predicate](/documents/arguments/block_predicate/en.md).
    - Same as above.
- `/testarg block_predicate <entity predicate> [string|nbt|json|string_test|nbt_test|json_test]`: [Entity predicate](/documents/arguments/entity_predicate/en.md).
    - Same as above.
- `/testarg nbt <NBT>`: Display the result of NBT pretty-printing (similar to what's output by `/data get`).
    - `... plainstring`: Display the result of NBT directly converted to string NBT (SNBT).
    - `... prettyprinted`: Display the result of NBT pretty-printing.
    - `... indented`: Display the result of NBT pretty-printed with indent.
    - `... test`: Parse the string form of NBT into an [NBT predicate](/documents/arguments/nbt_predicate/en.md) and an [NBT function](/documents/arguments/nbt_function/en.md), and test whether the NBT matches the NBT predicate, and test whether the result of applying the NBT function is identical to this NBT.
    - `... convert (block_function|block_predicate|entity_predicate|nbt_function|nbt_predicate|pos_argument|region)`: Decode NBT to objects of the corresponding type, and return the result.
- `/testart nbt_compound [nbt_test|json_test]`: Encode the NBT compound to NBT or JSON objects, and decode to NBT, and test whether the converted result is identical to before conversion.
- `/testarg nbt_function <NBT function>`: Display the result of the [NBT function](/documents/arguments/nbt_function/en.md) converted to string.
    - `... apply [NBT]`: Display the result of NBT function applied on empty contents or on a specified NBT.
    - `... [string|nbt|json|string_test|nbt_test|json_test]`
- `/testarg nbt_predicate <NBT predicate>`: Display the result of the [NBT predicate](/documents/arguments/nbt_predicate/en.md) converted to string.
    - `... [string|nbt|json|string_test|nbt_test|json_test]`
    - `... match <NBT to test>`: Test whether the NBT matches the specified NBT predicate.
- `/testarg pos ...`
    - `... (int_only|prefer_int|prefer_double|double_only) (unchanged|horizontally_centered|centered) <pos>`: Display the result of the specified pos argument evaluated according to the command source (such as the command executor's position).
        - `... [string|nbt|json|string_test|nbt_test|json_test]`: Same as above.
    - `... (vanilla_vec3|vanilla_vec3_accurate|vanilla_block_pos) <pos>`: See the calculation result of vanilla-type pos according to the command source.
- `/testarg region <region> [string|nbt|json|string_test|nbt_test|json_test]`: [Region](/documents/arguments/region/en.md).
    - Same as above.
- `/testart region <region> illustrate`: Illustrate a region with glass, and test whether there are issues in the region. If there is a block pos not included through the iteration of the region, but is in the region, the block pos will be shown with orange stained glass. If there is a block pos included during the iteration but not in the region, shown in red stained glass. In normal cases, no any red or orange stained glass should be made. This operation can be undone through [`/undo`](../undo/en.md).
