package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;

public record EffectEntityPredicateEntry(StatusEffect expected, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }
    final var actualEffects = livingEntity.getActiveStatusEffects();
    return actualEffects.containsKey(expected) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.effect.not_living"));
    }
    final var actualEffects = livingEntity.getActiveStatusEffects();
    final var actual = actualEffects.containsKey(expected);
    if (actual) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.effect.true_dummy", displayName, expected.getName()));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.effect.false_dummy", displayName, expected.getName()));
    }
  }

  @Override
  public String toOptionEntry() {
    return "effect=" + (inverted ? "!" : "") + Registries.STATUS_EFFECT.getId(expected);
  }
}
