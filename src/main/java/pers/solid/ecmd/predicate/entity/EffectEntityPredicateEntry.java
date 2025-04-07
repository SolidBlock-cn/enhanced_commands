package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;

public record EffectEntityPredicateEntry(StatusEffect expected, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
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
  public @Nullable String toOptionEntry() {
    return "effect=" + (inverted ? "!" : "") + Registries.STATUS_EFFECT.getId(expected);
  }
}
