package pers.solid.ecmd.api;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组对方块状态进行上下的翻转时的事件，用于在 {@link #getMirroredState(BlockState, Direction.Axis)} 中在进行上下翻转时使用。对于水平方向的翻转，请直接使用 {@link BlockState#mirror(Mirror)} 方法。
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
    if (original.hasProperty(BlockStateProperties.HALF)) {
      intermediate = intermediate.setValue(BlockStateProperties.HALF, switch (original.getValue(BlockStateProperties.HALF)) {
        case TOP -> Half.BOTTOM;
        case BOTTOM -> Half.TOP;
      });
    }
    if (original.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
      intermediate = intermediate.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, switch (original.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
        case LOWER -> DoubleBlockHalf.UPPER;
        case UPPER -> DoubleBlockHalf.LOWER;
      });
    }
    if (original.hasProperty(BlockStateProperties.SLAB_TYPE)) {
      intermediate = intermediate.setValue(BlockStateProperties.SLAB_TYPE, switch (original.getValue(BlockStateProperties.SLAB_TYPE)) {
        case TOP -> SlabType.BOTTOM;
        case BOTTOM -> SlabType.TOP;
        case DOUBLE -> SlabType.DOUBLE;
      });
    }
    if (original.hasProperty(BlockStateProperties.FACING)) {
      final Direction direction = original.getValue(BlockStateProperties.FACING);
      intermediate = intermediate.setValue(BlockStateProperties.FACING, switch (direction) {
        case UP -> Direction.DOWN;
        case DOWN -> Direction.UP;
        default -> direction;
      });
    }
    if (original.hasProperty(BlockStateProperties.UP) && original.hasProperty(BlockStateProperties.DOWN)) {
      intermediate = intermediate.setValue(BlockStateProperties.UP, original.getValue(BlockStateProperties.DOWN)).setValue(BlockStateProperties.DOWN, original.getValue(BlockStateProperties.UP));
    }
    if (original.hasProperty(BlockStateProperties.ATTACH_FACE)) {
      intermediate = intermediate.setValue(BlockStateProperties.ATTACH_FACE, switch (original.getValue(BlockStateProperties.ATTACH_FACE)) {
        case FLOOR -> AttachFace.CEILING;
        case CEILING -> AttachFace.FLOOR;
        case WALL -> AttachFace.WALL;
      });
    }
    return intermediate;
  };


  static BlockState getMirroredState(BlockState blockState, Direction.Axis axis) {
    return switch (axis) {
      case X -> blockState.mirror(Mirror.FRONT_BACK);
      case Z -> blockState.mirror(Mirror.LEFT_RIGHT);
      case Y -> EventBridges.INSTANCE.flipState().invoker().getFlippedState(blockState);
    };
  }

  @ApiStatus.Internal
  static void registerDefaultEvent() {
    EventBridges.INSTANCE.flipState().register(FlipStateCallback.DEFAULT);
  }
}
