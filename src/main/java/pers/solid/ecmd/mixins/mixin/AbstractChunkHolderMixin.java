package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.chunk.AbstractChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;

@Mixin(AbstractChunkHolder.class)
public abstract class AbstractChunkHolderMixin {
  @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ChunkPos;getChebyshevDistance(Lnet/minecraft/util/math/ChunkPos;)I"))
  private int ignoreChebyshevDistance(int original) {
    if (DebugIgnoreBoundaryCommand.ignoreBoundary) {
      return 0;
    }
    return original;
  }
}
