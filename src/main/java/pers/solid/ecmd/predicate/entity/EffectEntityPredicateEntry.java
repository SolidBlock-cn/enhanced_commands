package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record EffectEntityPredicateEntry(RegistryEntry<StatusEffect> expected, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<EffectEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      StatusEffect.ENTRY_CODEC.fieldOf("expected").forGetter(EffectEntityPredicateEntry::expected),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EffectEntityPredicateEntry::inverted)
  ).apply(i, EffectEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }
    final var actualEffects = livingEntity.getActiveStatusEffects();
    return actualEffects.containsKey(expected) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.effect.not_living"));
    }
    final var actualEffects = livingEntity.getActiveStatusEffects();
    final var actual = actualEffects.containsKey(expected);
    if (actual) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.effect.true_dummy", displayName, expected.value().getName()));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.effect.false_dummy", displayName, expected.value().getName()));
    }
  }

  @Override
  public @NotNull EntityPredicateType<EffectEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EFFECT;
  }

  @Override
  public String toOptionEntry() {
    return "effect=" + (inverted ? "!" : "") + expected.getIdAsString();
  }
}
