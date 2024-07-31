package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import pers.solid.ecmd.util.TestResult;

import java.util.*;

public record EffectsEntityPredicateEntry(Map<StatusEffect, EntityEffectPredicate.EffectData> effects, Set<StatusEffect> inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }/* // todo complete
    final var actualEffects = livingEntity.getActiveStatusEffects();
    for (final var entry : effects.entrySet()) {
      final StatusEffect statusEffect = entry.getKey();
      StatusEffectInstance statusEffectInstance = actualEffects.get(statusEffect);
      if (entry.getValue().test(statusEffectInstance) == inverted.contains(statusEffect)) {
        return false;
      }
    }*/
    return true;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.effect.not_living"));
    }
    final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> actualEffects = livingEntity.getActiveStatusEffects();
    boolean result = true;
    final List<TestResult> attachments = new ArrayList<>();
/* // todo complete effect
    for (final Map.Entry<StatusEffect, EntityEffectPredicate.EffectData> entry : effects.entrySet()) {
      final StatusEffect statusEffect = entry.getKey();
      StatusEffectInstance statusEffectInstance = actualEffects.get(statusEffect);
      final var testResult = entry.getValue().test(statusEffectInstance);
      final var isInverted = inverted.contains(statusEffect);
      final var passes = testResult != isInverted;
      result &= passes;

      final EffectDataAccessor accessor = (EffectDataAccessor) (Object) entry.getValue();
      if (accessor.getAmplifier().isDummy() && accessor.getDuration().isDummy() && accessor.getAmbient() == null && accessor.getVisible() == null) {
        if (testResult) {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.true_dummy", displayName, statusEffect.getName())));
        } else {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.false_dummy", displayName, statusEffect.getName())));
        }
      } else {
        if (testResult) {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.true_advanced", displayName, statusEffect.getName())));
        } else if (statusEffectInstance != null) {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.false_advanced", displayName, statusEffect.getName())));
        } else {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.false_advanced_no_effect", displayName, statusEffect.getName())));
        }
      }
    }*/
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.effect.pass", displayName), attachments);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.effect.fail", displayName), attachments);
    }
  }

  @Override
  public String toOptionEntry() {
    final StringJoiner joiner = new StringJoiner(", ", "{", "}");/* // todo complete
    for (var entry : effects.entrySet()) {
      final StatusEffect statusEffect = entry.getKey();
      final Identifier id = Registries.STATUS_EFFECT.getId(statusEffect);
      final EntityEffectPredicate.EffectData effectData = entry.getValue();
      final EffectDataAccessor accessor = (EffectDataAccessor) (Object) effectData;
      final StringJoiner joiner2 = new StringJoiner(", ", "{", "}");
      final NumberRange.IntRange amplifier = accessor.getAmplifier();
      boolean dummy = true;
      if (!amplifier.isDummy()) {
        joiner2.add("amplifier = " + StringUtil.wrapRange(amplifier));
        dummy = false;
      }
      final NumberRange.IntRange duration = accessor.getDuration();
      if (!duration.isDummy()) {
        joiner2.add("duration = " + StringUtil.wrapRange(duration));
        dummy = false;
      }
      final Boolean ambient = accessor.getAmbient();
      if (ambient != null) {
        joiner2.add("ambient = " + ambient);
        dummy = false;
      }
      final Boolean visible = accessor.getVisible();
      if (visible != null) {
        joiner2.add("visible = " + visible);
        dummy = false;
      }

      if (dummy) {
        joiner.add(id + " = " + inverted.contains(statusEffect));
      } else {
        joiner.add(id + " = " + (inverted.contains(statusEffect) ? "!" : "") + joiner2);
      }
    }*/

    return joiner.toString();
  }
}
