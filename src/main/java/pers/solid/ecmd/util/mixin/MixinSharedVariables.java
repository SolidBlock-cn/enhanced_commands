package pers.solid.ecmd.util.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import pers.solid.ecmd.command.FillReplaceCommand;

public final class MixinSharedVariables {
  public static final ImmutableMap<String, GameMode> EXTENDED_GAME_MODE_NAMES = ImmutableMap.of(
      "s", GameMode.SURVIVAL,
      "c", GameMode.CREATIVE,
      "a", GameMode.ADVENTURE,
      "sp", GameMode.SPECTATOR,
      "0", GameMode.SURVIVAL,
      "1", GameMode.CREATIVE,
      "2", GameMode.ADVENTURE,
      "3", GameMode.SPECTATOR);
  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.chunk.WorldChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onBlockAdded(World, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   *
   * @see pers.solid.ecmd.mixin.WorldChunkMixin#wrappedOnBlockAdded(BlockState, World, BlockPos, BlockState, boolean)
   */
  public static boolean suppressOnBlockAdded = false;

  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.chunk.WorldChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onStateReplaced(World, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   */
  public static boolean suppressOnStateReplaced = false;

  public static void implementModFlag(int modFlags) {
    MixinSharedVariables.suppressOnBlockAdded = (modFlags & FillReplaceCommand.SUPPRESS_INITIAL_CHECK_FLAG) != 0;
    MixinSharedVariables.suppressOnStateReplaced = (modFlags & FillReplaceCommand.SUPPRESS_REPLACED_CHECK_FLAG) != 0;
  }

  public static void releaseModFlag() {
    MixinSharedVariables.suppressOnBlockAdded = false;
    MixinSharedVariables.suppressOnStateReplaced = false;
  }

  public static boolean setBlockStateWithModFlags(World world, BlockPos blockPos, BlockState blockState, int flags, int modFlags) {
    MixinSharedVariables.implementModFlag(modFlags);
    boolean result;
    try {
      result = world.setBlockState(blockPos, blockState, flags);
    } finally {
      MixinSharedVariables.releaseModFlag();
    }
    return result;
  }

  private MixinSharedVariables() {
  }
}
