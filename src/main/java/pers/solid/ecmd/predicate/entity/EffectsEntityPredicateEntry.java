package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TestResult;

import java.util.*;

public record EffectsEntityPredicateEntry(List<Entry> effects) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<EffectsEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Entry.CODEC.listOf().fieldOf("effects").forGetter(EffectsEntityPredicateEntry::effects)
  ).apply(i, EffectsEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }
    final var actualEffects = livingEntity.getActiveStatusEffects();
    for (final var entry : effects) {
      final RegistryEntry<StatusEffect> statusEffect = entry.effect;
      StatusEffectInstance statusEffectInstance = actualEffects.get(statusEffect);
      if (entry.data.test(statusEffectInstance) == entry.expected) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.effect.not_living"));
    }
    final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> actualEffects = livingEntity.getActiveStatusEffects();
    boolean result = true;
    final List<TestResult> attachments = new ArrayList<>();
    for (final var entry : effects) {
      final RegistryEntry<StatusEffect> effectEntry = entry.effect;
      StatusEffectInstance statusEffectInstance = actualEffects.get(effectEntry);
      final EntityEffectPredicate.EffectData effectData = entry.data;
      final var testResult = effectData.test(statusEffectInstance);
      final var expected = entry.expected;
      final var passes = testResult == expected;
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
  public @NotNull EntityPredicateType<EffectsEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EFFECTS;
  }

  @Override
  public String toOptionEntry() {
    final StringJoiner joiner = new StringJoiner(", ", "{", "}");
    for (var entry : effects) {
      joiner.add(entry.asString());
    }

    return "effect=" + joiner;
  }

  public record Entry(RegistryEntry<StatusEffect> effect, EntityEffectPredicate.EffectData data, boolean expected) implements ExpressionConvertible {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
        StatusEffect.ENTRY_CODEC.fieldOf("effect").forGetter(Entry::effect),
        EntityEffectPredicate.EffectData.CODEC.fieldOf("data").forGetter(Entry::data),
        Codec.BOOL.optionalFieldOf("expected", true).forGetter(Entry::expected)
    ).apply(i, Entry::new));

    @Override
    public @NotNull String asString() {
      final RegistryEntry<StatusEffect> effectEntry = effect;
      final EntityEffectPredicate.EffectData effectData = data;
      final StringJoiner joiner = new StringJoiner(", ", "{", "}");
      final NumberRange.IntRange amplifier = effectData.amplifier();
      boolean dummy = true;
      if (!amplifier.isDummy()) {
        joiner.add("amplifier=" + StringUtil.wrapRange(amplifier));
        dummy = false;
      }
      final NumberRange.IntRange duration = effectData.duration();
      if (!duration.isDummy()) {
        joiner.add("duration=" + StringUtil.wrapRange(duration));
        dummy = false;
      }
      final Optional<Boolean> ambient = effectData.ambient();
      if (ambient.isPresent()) {
        joiner.add("ambient=" + ambient);
        dummy = false;
      }
      final Optional<Boolean> visible = effectData.visible();
      if (visible.isPresent()) {
        joiner.add("visible=" + visible);
        dummy = false;
      }

      final String effectId = effectEntry.getKeyOrValue().map(key -> key.getValue().toString(), statusEffect -> StatusEffect.ENTRY_CODEC.encodeStart(NbtOps.INSTANCE, effectEntry).getOrThrow().toString());
      if (dummy) {
        return effectId + "=" + expected;
      } else {
        return effectId + "=" + (expected ? "!" : "") + joiner;
      }
    }
  }
}
