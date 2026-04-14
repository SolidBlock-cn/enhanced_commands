package pers.solid.ecmd.entity.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record EffectEntityPredicateEntry(Holder<MobEffect> effect, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<EffectEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      MobEffect.CODEC.fieldOf("effect").forGetter(EffectEntityPredicateEntry::effect),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EffectEntityPredicateEntry::inverted)
  ).apply(i, EffectEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }
    final var actualEffects = livingEntity.getActiveEffectsMap();
    return actualEffects.containsKey(effect) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.effect.not_living"));
    }
    final var actualEffects = livingEntity.getActiveEffectsMap();
    final var actual = actualEffects.containsKey(effect);
    if (actual) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.effect.true_dummy", displayName, effect.value().getDisplayName()));
    } else {
      return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.effect.false_dummy", displayName, effect.value().getDisplayName()));
    }
  }

  @Override
  public @NotNull EntityPredicateType<EffectEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EFFECT;
  }

  @Override
  public String toOptionEntry() {
    return "effect=" + (inverted ? "!" : "") + effect.getRegisteredName();
  }
}
