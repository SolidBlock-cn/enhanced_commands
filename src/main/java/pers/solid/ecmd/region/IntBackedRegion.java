package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Function;

/**
 * A region that supports only integer operations. Operations related to double will throw {@link UnsupportedOperationException}, unless it equals to the integer.
 */
public interface IntBackedRegion extends Region {
  DynamicCommandExceptionType MOVE_MUST_INT = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.exception.move_must_int", o));
  DynamicCommandExceptionType MOVE_MUST_INT_VECTOR = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.exception.move_must_int_vector", o));
  DynamicCommandExceptionType EXPAND_MUST_INT = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.exception.expand_must_int", o));
  DynamicCommandExceptionType ROTATION_PIVOT_MUST_CENTER = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.exception.rotation_pivot_must_center", o));
  DynamicCommandExceptionType MIRROR_PIVOT_MUST_CENTER = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.exception.mirror_pivot_must_center", o));

  static Vec3i toFlooredIntOrThrow(Vec3 vec3d, DynamicCommandExceptionType dynamicCommandExceptionType) {
    final BlockPos vec3i = BlockPos.containing(vec3d);
    if (vec3d.equals(Vec3.atLowerCornerOf(vec3i))) {
      return vec3i;
    } else {
      throw new UnsupportedOperationException(dynamicCommandExceptionType.create(TextUtil.wrapVector(vec3d).withStyle(Styles.ACTUAL)));
    }
  }

  static Vec3i toCenteredIntOrThrow(Vec3 vec3d, DynamicCommandExceptionType dynamicCommandExceptionType) {
    final BlockPos vec3i = BlockPos.containing(vec3d);
    if (vec3d.equals(Vec3.atCenterOf(vec3i))) {
      return vec3i;
    } else {
      throw new UnsupportedOperationException(dynamicCommandExceptionType.create(TextUtil.wrapVector(vec3d).withStyle(Styles.ACTUAL)));
    }
  }

  @Override
  boolean contains(@NotNull Vec3i vec3i);

  @Override
  default boolean contains(@NotNull Vec3 vec3d) {
    return contains(BlockPos.containing(vec3d));
  }

  @Override
  @NotNull
  default Region moved(@NotNull Vec3i relativePos) {
    return transformedInt(vec3i -> vec3i.offset(relativePos));
  }

  @Override
  @NotNull
  default Region moved(@NotNull Vec3 relativePos) {
    return moved(toFlooredIntOrThrow(relativePos, MOVE_MUST_INT_VECTOR));
  }

  @Override
  @NotNull
  default Region expanded(double offset) {
    if (offset == (int) offset) {
      return expanded((int) offset);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).withStyle(Styles.ACTUAL)));
    }
  }

  default Region expanded(int offset) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region expanded(double offset, Direction.Axis axis) {
    if (offset == (int) offset) {
      return expanded((int) offset, axis);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).withStyle(Styles.ACTUAL)));
    }
  }

  default Region expanded(int offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region expanded(double offset, Direction direction) {
    if (offset == (int) offset) {
      return expanded((int) offset, direction);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).withStyle(Styles.ACTUAL)));
    }
  }

  default Region expanded(int offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region expanded(double offset, Direction.Plane type) {
    if (offset == (int) offset) {
      return expanded((int) offset, type);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).withStyle(Styles.ACTUAL)));
    }
  }

  default Region expanded(int offset, Direction.Plane type) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region rotated(@NotNull Rotation blockRotation, @NotNull Vec3 pivot) {
    return rotated(toCenteredIntOrThrow(pivot, ROTATION_PIVOT_MUST_CENTER), blockRotation);
  }

  @NotNull
  default Region rotated(@NotNull Vec3i pivot, @NotNull Rotation blockRotation) {
    return transformedInt(vec3i -> GeoUtil.rotate(vec3i, blockRotation, pivot));
  }

  Region transformedInt(Function<Vec3i, Vec3i> transformation);

  @Override
  default @NotNull Region transformed(Function<Vec3, Vec3> transformation) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region mirrored(Direction.@NotNull Axis axis, @NotNull Vec3 pivot) {
    return mirrored(toCenteredIntOrThrow(pivot, MIRROR_PIVOT_MUST_CENTER), axis);
  }

  @NotNull
  default Region mirrored(Vec3i pivot, Direction.@NotNull Axis axis) {
    return transformedInt(vec3i -> GeoUtil.mirror(vec3i, axis, pivot));
  }

  @Override
  long numberOfBlocksAffected();

  @Override
  default double volume() {
    return numberOfBlocksAffected();
  }

  @Override
  @Nullable
  BoundingBox minContainingBlockBox();

  @Override
  @Nullable
  default AABB minContainingBox() {
    final BoundingBox blockBox = minContainingBlockBox();
    if (blockBox == null) {
      return null;
    } else {
      return AABB.of(blockBox);
    }
  }
}
