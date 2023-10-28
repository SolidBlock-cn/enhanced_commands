package pers.solid.ecmd.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组对方块状态进行上下的翻转时的事件，用于在 {@link #getMirroredState(BlockState, Direction.Axis)} 中在进行上下翻转时使用。对于水平方向的翻转，请直接使用 {@link BlockState#mirror(BlockMirror)} 方法。
 *
 * @see #DEFAULT
 */
@FunctionalInterface
public interface FlipStateCallback {
  /**
   * 实现此方法以对特定的方块状态进行修改。返回 {@code intermediate} 即表示不修改。为避免潜在的重复操作而导致不能正确返回的问题，请对 {@code original} 中的属性的值进行操作，然后再返回 {@code intermedia.with(...)}。例如：
   * <pre>{@code
   *  // 正确
   *  return intermediate.with(FACING, transform(original.get(FACING)));
   *
   *  // 错误
   *  return intermediate.with(FACING, transform(intermediate.get(FACING)));
   * }</pre>
   *
   * @param intermediate 在一整个事件调用中所使用的方块状态。它可能已经受到之前注册的此 API 的事件的影响，而与原先的方块状态不相同。需要表示不对方块状态进行修改时，请直接返回它。
   * @param original     在一整个事件调用之前的方块状态。
   * @return 上下翻转后的方块状态。
   */
  @NotNull BlockState getFlippedState(@NotNull BlockState intermediate, @NotNull BlockState original);

  /**
   * 上下翻转方块状态，此方法不能实现多个事件的嵌套。请不要覆盖此方法。
   */
  @ApiStatus.NonExtendable
  default @NotNull BlockState getFlippedState(@NotNull BlockState blockState) {
    return getFlippedState(blockState, blockState);
  }

  /**
   * 本模组中默认为方块注册的一些事件，用于实现原版方块中的一些上下翻转。
   */
  FlipStateCallback DEFAULT = (intermediate, original) -> {
    if (original.contains(Properties.BLOCK_HALF)) {
      intermediate = intermediate.with(Properties.BLOCK_HALF, switch (original.get(Properties.BLOCK_HALF)) {
        case TOP -> BlockHalf.BOTTOM;
        case BOTTOM -> BlockHalf.TOP;
      });
    }
    if (original.contains(Properties.DOUBLE_BLOCK_HALF)) {
      intermediate = intermediate.with(Properties.DOUBLE_BLOCK_HALF, switch (original.get(Properties.DOUBLE_BLOCK_HALF)) {
        case LOWER -> DoubleBlockHalf.UPPER;
        case UPPER -> DoubleBlockHalf.LOWER;
      });
    }
    if (original.contains(Properties.SLAB_TYPE)) {
      intermediate = intermediate.with(Properties.SLAB_TYPE, switch (original.get(Properties.SLAB_TYPE)) {
        case TOP -> SlabType.BOTTOM;
        case BOTTOM -> SlabType.TOP;
        case DOUBLE -> SlabType.DOUBLE;
      });
    }
    if (original.contains(Properties.FACING)) {
      final Direction direction = original.get(Properties.FACING);
      intermediate = intermediate.with(Properties.FACING, switch (direction) {
        case UP -> Direction.DOWN;
        case DOWN -> Direction.UP;
        default -> direction;
      });
    }
    if (original.contains(Properties.UP) && original.contains(Properties.DOWN)) {
      intermediate = intermediate.with(Properties.UP, original.get(Properties.DOWN)).with(Properties.DOWN, original.get(Properties.UP));
    }
    if (original.contains(Properties.WALL_MOUNT_LOCATION)) {
      intermediate = intermediate.with(Properties.WALL_MOUNT_LOCATION, switch (original.get(Properties.WALL_MOUNT_LOCATION)) {
        case FLOOR -> WallMountLocation.CEILING;
        case CEILING -> WallMountLocation.FLOOR;
        case WALL -> WallMountLocation.WALL;
      });
    }
    return intermediate;
  };

  Event<FlipStateCallback> EVENT = EventFactory.createArrayBacked(FlipStateCallback.class, flipStateEvents -> (intermediate, original) -> {
    for (FlipStateCallback flipStateCallback : flipStateEvents) {
      intermediate = flipStateCallback.getFlippedState(intermediate, original);
    }
    return intermediate;
  });

  static void registerDefaultEvent() {
    EVENT.register(DEFAULT);
  }

  static BlockState getMirroredState(BlockState blockState, Direction.Axis axis) {
    return switch (axis) {
      case X -> blockState.mirror(BlockMirror.FRONT_BACK);
      case Z -> blockState.mirror(BlockMirror.LEFT_RIGHT);
      case Y -> EVENT.invoker().getFlippedState(blockState);
    };
  }
}
