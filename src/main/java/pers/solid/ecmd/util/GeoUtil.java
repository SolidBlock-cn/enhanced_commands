package pers.solid.ecmd.util;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public interface GeoUtil {
  static Vec3i rotate(Vec3i pos, BlockRotation rotation, Vec3i pivot) {
    return transform(pos, BlockMirror.NONE, rotation, pivot);
  }

  static Vec3d rotate(Vec3d point, BlockRotation rotation, Vec3d pivot) {
    return transform(point, BlockMirror.NONE, rotation, pivot);
  }

  /**
   * @see net.minecraft.structure.StructureTemplate#transformAround(BlockPos, BlockMirror, BlockRotation, BlockPos)
   */
  static Vec3i transform(Vec3i pos, BlockMirror mirror, BlockRotation rotation, Vec3i pivot) {
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    boolean useModifiedPos = true;
    switch (mirror) {
      case LEFT_RIGHT -> z = -z;
      case FRONT_BACK -> x = -x;
      default -> useModifiedPos = false;
    }

    int l = pivot.getX();
    int m = pivot.getZ();
    return switch (rotation) {
      case COUNTERCLOCKWISE_90 -> new Vec3i(l - m + z, y, l + m - x);
      case CLOCKWISE_90 -> new Vec3i(l + m - z, y, m - l + x);
      case CLOCKWISE_180 -> new Vec3i(l + l - x, y, m + m - z);
      default -> useModifiedPos ? new Vec3i(x, y, z) : pos;
    };
  }

  /**
   * @see net.minecraft.structure.StructureTemplate#transformAround(Vec3d, BlockMirror, BlockRotation, BlockPos)
   */
  static Vec3d transform(Vec3d point, BlockMirror mirror, BlockRotation rotation, Vec3d pivot) {
    double x = point.x;
    double y = point.y;
    double z = point.z;
    boolean useModifiedPoint = true;
    switch (mirror) {
      case LEFT_RIGHT -> z = -z;
      case FRONT_BACK -> x = -x;
      default -> useModifiedPoint = false;
    }

    double i = pivot.getX();
    double j = pivot.getZ();
    return switch (rotation) {
      case COUNTERCLOCKWISE_90 -> new Vec3d((i - j) + z, y, (i + j + 1) - x);
      case CLOCKWISE_90 -> new Vec3d((i + j + 1) - z, y, (j - i) + x);
      case CLOCKWISE_180 -> new Vec3d((i + i + 1) - x, y, (j + j + 1) - z);
      default -> useModifiedPoint ? new Vec3d(x, y, z) : point;
    };
  }

  static Vec3i mirror(Vec3i point, Direction.Axis axis, Vec3i pivot) {
    return switch (axis) {
      case X -> new Vec3i(pivot.getX() * 2 - point.getX(), point.getY(), point.getZ());
      case Y -> new Vec3i(point.getX(), pivot.getY() * 2 - point.getY(), point.getZ());
      case Z -> new Vec3i(point.getX(), point.getY(), pivot.getZ() * 2 - point.getZ());
    };
  }

  static Vec3d mirror(Vec3d point, Direction.Axis axis, Vec3d pivot) {
    return switch (axis) {
      case X -> new Vec3d(pivot.x * 2 - point.x, point.y, point.z);
      case Y -> new Vec3d(point.x, pivot.y * 2 - point.y, point.z);
      case Z -> new Vec3d(point.x, point.y, pivot.z * 2 - point.z);
    };
  }
}
