package pers.solid.ecmd.block;

import net.minecraft.util.math.BlockPos;

/**
 * 在执行方块操作时遇到没有加载的区块而进行的中断操作。
 */
public final class UnloadedPosException extends RuntimeException {
  public final BlockPos unloadedPos;

  UnloadedPosException(BlockPos unloadedPos) {this.unloadedPos = unloadedPos;}
}
