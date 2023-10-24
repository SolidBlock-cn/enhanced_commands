package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TextUtil;

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

  DynamicCommandExceptionType MOVE_MUST_INT = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.exception.move_must_int", o));

  @Override
  @NotNull
  default Region moved(double count, @NotNull Direction direction) {
    if (count == (int) count) {
      return moved((int) count, direction);
    } else {
      throw new UnsupportedOperationException(MOVE_MUST_INT.create(Text.literal(Double.toString(count)).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  @Override
  @NotNull
  Region moved(@NotNull Vec3i relativePos);

  DynamicCommandExceptionType MOVE_MUST_INT_VECTOR = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.exception.move_must_int_vector", o));

  @Override
  @NotNull
  default Region moved(@NotNull Vec3d relativePos) {
    return moved(toFlooredIntOrThrow(relativePos, MOVE_MUST_INT_VECTOR));
  }

  DynamicCommandExceptionType EXPAND_MUST_INT = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.exception.expand_must_int", o));

  @Override
  @NotNull
  default Region expanded(double offset) {
    if (offset == (int) offset) {
      return expanded((int) offset);
    } else {
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(Text.literal(Double.toString(offset)).styled(TextUtil.STYLE_FOR_ACTUAL)));
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
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(Text.literal(Double.toString(offset)).styled(TextUtil.STYLE_FOR_ACTUAL)));
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
      throw new UnsupportedOperationException(EXPAND_MUST_INT.create(Text.literal(Double.toString(offset)).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  default Region expanded(int offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  DynamicCommandExceptionType ROTATION_PIVOT_MUST_CENTER = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.exception.rotation_pivot_must_center", o));

  @Override
  @NotNull
  default Region rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return rotated(toCenteredIntOrThrow(pivot, ROTATION_PIVOT_MUST_CENTER), blockRotation);
  }

  @NotNull Region rotated(@NotNull Vec3i pivot, @NotNull BlockRotation blockRotation);

  DynamicCommandExceptionType MIRROR_PIVOT_MUST_CENTER = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.exception.mirror_pivot_must_center", o));

  @Override
  @NotNull
  default Region mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return mirrored(toCenteredIntOrThrow(pivot, MIRROR_PIVOT_MUST_CENTER), axis);
  }

  @NotNull Region mirrored(Vec3i pivot, Direction.@NotNull Axis axis);

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
      throw new UnsupportedOperationException(dynamicCommandExceptionType.create(TextUtil.wrapPosition(vec3d).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  static Vec3i toCenteredIntOrThrow(Vec3d vec3d, DynamicCommandExceptionType dynamicCommandExceptionType) {
    final BlockPos vec3i = BlockPos.ofFloored(vec3d);
    if (vec3d.equals(Vec3d.ofCenter(vec3i))) {
      return vec3i;
    } else {
      throw new UnsupportedOperationException(dynamicCommandExceptionType.create(TextUtil.wrapPosition(vec3d).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  @Override
  @Nullable BlockBox maxContainingBlockBox();

  @Override
  @Nullable
  default Box minContainingBox() {
    final BlockBox blockBox = maxContainingBlockBox();
    if (blockBox == null) {
      return null;
    } else {
      return Box.from(blockBox);
    }
  }
}
