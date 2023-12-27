package pers.solid.ecmd.mixin;

import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityEffectPredicate.EffectData.class)
public interface EffectDataAccessor {
  @Accessor
  NumberRange.IntRange getAmplifier();

  @Accessor
  NumberRange.IntRange getDuration();

  @Accessor
  Boolean getAmbient();

  @Accessor
  Boolean getVisible();

}
