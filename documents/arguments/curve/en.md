# Curve

**Curve** is an [argument type](../en.md) which is a curve in the world, and specifies how to iterate points in the world, to draw this curve.

Different from regions, curves have length, do not have volume, and cannot test whether a specified point is within the curve. The points iterated are precise points, instead of block pos.

## Curve types

There are the following curve types:

- [`circle(...)`](circle/en.md): A full circle, or part of circle.
- [`straight(<point 1>, <point 2>)`](straight/en.md): Curve specified by two points.

## Data structure

- `type`: String, id of the curve type, supporting the following values (all namespaced `enhanced_commands`, namespace is omitted in the following list):
    - `circle`
    - `straight`