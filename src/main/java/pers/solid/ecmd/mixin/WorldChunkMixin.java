package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {
  @WrapWithCondition(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onBlockAdded(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V"))
  public boolean wrappedOnBlockAdded(BlockState instance, World world, BlockPos blockPos, BlockState blockState, boolean moved) {
    return !MixinSharedVariables.suppressOnBlockAdded;
  }

  @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onStateReplaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V"))
  public void wrappedOnStateReplaced(BlockState instance, World world, BlockPos blockPos, BlockState newState, boolean moved, Operation<Void> operation) {
    if (MixinSharedVariables.suppressOnStateReplaced) {
      // 相当于 onStateReplaced 的基本的方法，必须先移除原有的方块实体以免出错
      if (instance.hasBlockEntity() && !instance.isOf(newState.getBlock())) {
        world.removeBlockEntity(blockPos);
      }
    } else {
      operation.call(instance, world, blockPos, newState, moved);
    }
  }
}
