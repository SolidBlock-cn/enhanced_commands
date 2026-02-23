package pers.solid.ecmd.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

public interface GeoUtil {
  static Vec3i rotate(Vec3i pos, Rotation rotation, Vec3i pivot) {
    return transform(pos, Mirror.NONE, rotation, pivot);
  }

  static Vec3 rotate(Vec3 point, Rotation rotation, Vec3 pivot) {
    return transform(point, Mirror.NONE, rotation, pivot);
  }

  /**
   * @see net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate#transform(BlockPos, Mirror, Rotation, BlockPos)
   */
  static Vec3i transform(Vec3i pos, Mirror mirror, Rotation rotation, Vec3i pivot) {
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    boolean useModifiedPos = true;
    switch (mirror) {
      case LEFT_RIGHT -> z = -z;
      case FRONT_BACK -> x = -x;
      default -> useModifiedPos = false;
    }

    int pivotX = pivot.getX();
    int pivotZ = pivot.getZ();
    return switch (rotation) {
      case COUNTERCLOCKWISE_90 -> new Vec3i(pivotX - pivotZ + z, y, pivotX + pivotZ - x);
      case CLOCKWISE_90 -> new Vec3i(pivotX + pivotZ - z, y, pivotZ - pivotX + x);
      case CLOCKWISE_180 -> new Vec3i(pivotX + pivotX - x, y, pivotZ + pivotZ - z);
      default -> useModifiedPos ? new Vec3i(x, y, z) : pos;
    };
  }

  /**
   * @see net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate#transform(Vec3, Mirror, Rotation, BlockPos)
   */
  static Vec3 transform(Vec3 point, Mirror mirror, Rotation rotation, Vec3 pivot) {
    double x = point.x;
    double y = point.y;
    double z = point.z;
    boolean useModifiedPoint = true;
    switch (mirror) {
      case LEFT_RIGHT -> z = -z;
      case FRONT_BACK -> x = -x;
      default -> useModifiedPoint = false;
    }

    double i = pivot.x();
    double j = pivot.z();
    return switch (rotation) {
      case COUNTERCLOCKWISE_90 -> new Vec3((i - j) + z, y, (i + j) - x);
      case CLOCKWISE_90 -> new Vec3((i + j) - z, y, (j - i) + x);
      case CLOCKWISE_180 -> new Vec3((i + i) - x, y, (j + j) - z);
      default -> useModifiedPoint ? new Vec3(x, y, z) : point;
    };
  }

  static Vec3i mirror(Vec3i point, Direction.Axis axis, Vec3i pivot) {
    return switch (axis) {
      case X -> new Vec3i(pivot.getX() * 2 - point.getX(), point.getY(), point.getZ());
      case Y -> new Vec3i(point.getX(), pivot.getY() * 2 - point.getY(), point.getZ());
      case Z -> new Vec3i(point.getX(), point.getY(), pivot.getZ() * 2 - point.getZ());
    };
  }

  static Vec3 mirror(Vec3 point, Direction.Axis axis, Vec3 pivot) {
    return switch (axis) {
      case X -> new Vec3(pivot.x * 2 - point.x, point.y, point.z);
      case Y -> new Vec3(point.x, pivot.y * 2 - point.y, point.z);
      case Z -> new Vec3(point.x, point.y, pivot.z * 2 - point.z);
    };
  }
}
