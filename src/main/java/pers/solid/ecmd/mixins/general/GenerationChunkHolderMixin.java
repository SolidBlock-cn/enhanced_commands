package pers.solid.ecmd.mixins.general;

import net.minecraft.server.level.GenerationChunkHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GenerationChunkHolder.class)
public abstract class GenerationChunkHolderMixin {
  // todo ignoreChessboardDistance
}
