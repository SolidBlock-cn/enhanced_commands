package pers.solid.ecmd.util.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MixinSharedVariables {
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

  private MixinSharedVariables() {}
}
