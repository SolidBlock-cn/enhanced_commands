package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtOps;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TestResult;

import java.util.*;

public record EffectsEntityPredicateEntry(Map<RegistryEntry<StatusEffect>, EntityEffectPredicate.EffectData> effects, Set<RegistryEntry<StatusEffect>> inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }
    final var actualEffects = livingEntity.getActiveStatusEffects();
    for (final var entry : effects.entrySet()) {
      final RegistryEntry<StatusEffect> statusEffect = entry.getKey();
      StatusEffectInstance statusEffectInstance = actualEffects.get(statusEffect);
      if (entry.getValue().test(statusEffectInstance) == inverted.contains(statusEffect)) {
        return false;
      }
    }
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
    for (final var mapEntry : effects.entrySet()) {
      final RegistryEntry<StatusEffect> effectEntry = mapEntry.getKey();
      StatusEffectInstance statusEffectInstance = actualEffects.get(effectEntry);
      final EntityEffectPredicate.EffectData effectData = mapEntry.getValue();
      final var testResult = effectData.test(statusEffectInstance);
      final var isInverted = inverted.contains(effectEntry);
      final var passes = testResult != isInverted;
      result &= passes;

      final Text effectName = effectEntry.value().getName();
      if (effectData.amplifier().isDummy() && effectData.duration().isDummy() && effectData.ambient().isEmpty() && effectData.visible().isEmpty()) {
        if (testResult) {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.true_dummy", displayName, effectName)));
        } else {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.false_dummy", displayName, effectName)));
        }
      } else {
        if (testResult) {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.true_advanced", displayName, effectName)));
        } else if (statusEffectInstance != null) {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.false_advanced", displayName, effectName)));
        } else {
          attachments.add(TestResult.of(passes, Text.translatable("enhanced_commands.entity_predicate.effect.false_advanced_no_effect", displayName, effectName)));
        }
      }
    }
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.effect.pass", displayName), attachments);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.effect.fail", displayName), attachments);
    }
  }

  @Override
  public String toOptionEntry() {
    final StringJoiner joiner = new StringJoiner(", ", "{", "}");
    for (var mapEntry : effects.entrySet()) {
      final RegistryEntry<StatusEffect> effectEntry = mapEntry.getKey();
      final EntityEffectPredicate.EffectData effectData = mapEntry.getValue();
      final StringJoiner joiner2 = new StringJoiner(", ", "{", "}");
      final NumberRange.IntRange amplifier = effectData.amplifier();
      boolean dummy = true;
      if (!amplifier.isDummy()) {
        joiner2.add("amplifier = " + StringUtil.wrapRange(amplifier));
        dummy = false;
      }
      final NumberRange.IntRange duration = effectData.duration();
      if (!duration.isDummy()) {
        joiner2.add("duration = " + StringUtil.wrapRange(duration));
        dummy = false;
      }
      final Optional<Boolean> ambient = effectData.ambient();
      if (ambient.isPresent()) {
        joiner2.add("ambient = " + ambient);
        dummy = false;
      }
      final Optional<Boolean> visible = effectData.visible();
      if (visible.isPresent()) {
        joiner2.add("visible = " + visible);
        dummy = false;
      }

      final String effectId = effectEntry.getKeyOrValue().map(key -> key.getValue().toString(), statusEffect -> StatusEffect.ENTRY_CODEC.encodeStart(NbtOps.INSTANCE, effectEntry).getOrThrow().toString());
      if (dummy) {
        joiner.add(effectId + " = " + inverted.contains(effectEntry));
      } else {
        joiner.add(effectId + " = " + (inverted.contains(effectEntry) ? "!" : "") + joiner2);
      }
    }

    return joiner.toString();
  }
}
