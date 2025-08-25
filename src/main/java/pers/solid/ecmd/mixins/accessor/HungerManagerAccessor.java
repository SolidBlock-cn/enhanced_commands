package pers.solid.ecmd.mixins.accessor;

import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HungerManager.class)
public interface HungerManagerAccessor {
  @Accessor
  float getExhaustion();

  @Accessor
  void setExhaustion(float exhaustion);
}
