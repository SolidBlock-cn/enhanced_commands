package pers.solid.ecmd.mixins.accessor;

import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(EntityEffectPredicate.EffectData.class)
public interface EffectDataAccessor {
  @Accessor
  NumberRange.IntRange getAmplifier();

  @Accessor
  NumberRange.IntRange getDuration();

  @Accessor
  Optional<Boolean> getAmbient();

  @Accessor
  Optional<Boolean> getVisible();

}
