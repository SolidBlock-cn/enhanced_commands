package pers.solid.ecmd.mixins.accessor;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataAccessor {
  @Accessor
  float getExhaustionLevel();

  @Accessor
  void setExhaustionLevel(float exhaustion);
}
