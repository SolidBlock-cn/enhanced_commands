package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Function;

/**
 * A region that supports only integer operations. Operations related double will throw {@link UnsupportedOperationException}, unless it equals to the integer.
 */
public interface IntBackedRegion extends Region {
  @Override
  boolean contains(@NotNull Vec3i vec3i);

  @Override
  default boolean contains(@NotNull Vec3d vec3d) {
    return contains(BlockPos.ofFloored(vec3d));
  }

  DynamicCommandExceptionType MOVE_MUST_INT = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.exception.move_must_int", o));

  @Override
  @NotNull
  default Region moved(@NotNull Vec3i relativePos) {
    return transformedInt(vec3i -> vec3i.add(relativePos));
  }

  DynamicCommandExceptionType MOVE_MUST_INT_VECTOR = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.exception.move_must_int_vector", o));

  @Override
  @NotNull
  default Region moved(@NotNull Vec3d relativePos) {
    return moved(toFlooredIntOrThrow(relativePos, MOVE_MUST_INT_VECTOR));
  }

  DynamicCommandExceptionType EXPAND_MUST_INT = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.exception.expand_must_int", o));

  @Override
  @NotNull
  default Region expanded(double offset) {
    if (offset == (int) offset) {
      return expanded((int) offset);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(Styles.ACTUAL)));
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
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(Styles.ACTUAL)));
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
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(Styles.ACTUAL)));
    }
  }

  default Region expanded(int offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region expanded(double offset, Direction.Type type) {
    if (offset == (int) offset) {
      return expanded((int) offset, type);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(Styles.ACTUAL)));
    }
  }

  default Region expanded(int offset, Direction.Type type) {
    throw new UnsupportedOperationException();
  }

  DynamicCommandExceptionType ROTATION_PIVOT_MUST_CENTER = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.exception.rotation_pivot_must_center", o));

  @Override
  @NotNull
  default Region rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return rotated(toCenteredIntOrThrow(pivot, ROTATION_PIVOT_MUST_CENTER), blockRotation);
  }

  @NotNull
  default Region rotated(@NotNull Vec3i pivot, @NotNull BlockRotation blockRotation) {
    return transformedInt(vec3i -> GeoUtil.rotate(vec3i, blockRotation, pivot));
  }

  Region transformedInt(Function<Vec3i, Vec3i> transformation);

  @Override
  default @NotNull Region transformed(Function<Vec3d, Vec3d> transformation) {
    throw new UnsupportedOperationException();
  }

  DynamicCommandExceptionType MIRROR_PIVOT_MUST_CENTER = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.exception.mirror_pivot_must_center", o));

  @Override
  @NotNull
  default Region mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
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

  static Vec3i toFlooredIntOrThrow(Vec3d vec3d, DynamicCommandExceptionType dynamicCommandExceptionType) {
    final BlockPos vec3i = BlockPos.ofFloored(vec3d);
    if (vec3d.equals(Vec3d.of(vec3i))) {
      return vec3i;
    } else {
      throw new UnsupportedOperationException(dynamicCommandExceptionType.create(TextUtil.wrapVector(vec3d).styled(Styles.ACTUAL)));
    }
  }

  static Vec3i toCenteredIntOrThrow(Vec3d vec3d, DynamicCommandExceptionType dynamicCommandExceptionType) {
    final BlockPos vec3i = BlockPos.ofFloored(vec3d);
    if (vec3d.equals(Vec3d.ofCenter(vec3i))) {
      return vec3i;
    } else {
      throw new UnsupportedOperationException(dynamicCommandExceptionType.create(TextUtil.wrapVector(vec3d).styled(Styles.ACTUAL)));
    }
  }

  @Override
  @Nullable
  BlockBox minContainingBlockBox();

  @Override
  @Nullable
  default Box minContainingBox() {
    final BlockBox blockBox = minContainingBlockBox();
    if (blockBox == null) {
      return null;
    } else {
      return Box.from(blockBox);
    }
  }
}
