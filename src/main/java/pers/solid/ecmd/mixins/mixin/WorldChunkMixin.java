package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.util.mixin.MixinShared;

@Mixin(LevelChunk.class)
public abstract class WorldChunkMixin {
  @WrapWithCondition(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"))
  public boolean wrappedOnBlockAdded(BlockState instance, Level world, BlockPos blockPos, BlockState blockState, boolean moved) {
    return !MixinShared.suppressOnBlockAdded;
  }

  @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"))
  public void wrappedOnStateReplaced(BlockState instance, Level world, BlockPos blockPos, BlockState newState, boolean moved, Operation<Void> operation) {
    if (MixinShared.suppressOnStateReplaced) {
      // 相当于 onStateReplaced 的基本的方法，必须先移除原有的方块实体以免出错
      if (instance.hasBlockEntity() && !instance.is(newState.getBlock())) {
        world.removeBlockEntity(blockPos);
      }
    } else {
      operation.call(instance, world, blockPos, newState, moved);
    }
  }
}
