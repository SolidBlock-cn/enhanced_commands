package pers.solid.ecmd.mixins.accessor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockInWorld.class)
public interface CachedBlockPositionAccessor {
  @Accessor
  void setState(BlockState state);
}
