package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.GenerationChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.config.DebugConfig;

@Mixin(GenerationChunkHolder.class)
public abstract class GenerationChunkHolderMixin {
  @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;getChessboardDistance(Lnet/minecraft/world/level/ChunkPos;)I"))
  private int ignoreChebyshevDistance(int original) {
    if (DebugConfig.current.ignoreBoundary) {
      return 0;
    }
    return original;
  }
}
