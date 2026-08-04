package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
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
  public boolean test(Entity entity) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return false;
    }
    final var actualEffects = livingEntity.getActiveEffectsMap();
    for (final var entry : effects) {
      final Holder<MobEffect> statusEffect = entry.effect;
      MobEffectInstance statusEffectInstance = actualEffects.get(statusEffect);
      if (entry.data.matches(statusEffectInstance) == entry.expected) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof final LivingEntity livingEntity)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.effect.not_living"));
    }
    final Map<Holder<MobEffect>, MobEffectInstance> actualEffects = livingEntity.getActiveEffectsMap();
    boolean result = true;
    final List<TestResult> attachments = new ArrayList<>();
    for (final var entry : effects) {
      final Holder<MobEffect> effectEntry = entry.effect;
      MobEffectInstance statusEffectInstance = actualEffects.get(effectEntry);
      final MobEffectsPredicate.MobEffectInstancePredicate effectData = entry.data;
      final var testResult = effectData.matches(statusEffectInstance);
      final var expected = entry.expected;
      final var passes = testResult == expected;
      result &= passes;

      final Component effectName = effectEntry.value().getDisplayName();
      if (effectData.amplifier().isAny() && effectData.duration().isAny() && effectData.ambient().isEmpty() && effectData.visible().isEmpty()) {
        if (testResult) {
          attachments.add(TestResult.of(passes, Component.translatable("enhanced_commands.entity_predicate.effect.true_dummy", displayName, effectName)));
        } else {
          attachments.add(TestResult.of(passes, Component.translatable("enhanced_commands.entity_predicate.effect.false_dummy", displayName, effectName)));
        }
      } else {
        if (testResult) {
          attachments.add(TestResult.of(passes, Component.translatable("enhanced_commands.entity_predicate.effect.true_advanced", displayName, effectName)));
        } else if (statusEffectInstance != null) {
          attachments.add(TestResult.of(passes, Component.translatable("enhanced_commands.entity_predicate.effect.false_advanced", displayName, effectName)));
        } else {
          attachments.add(TestResult.of(passes, Component.translatable("enhanced_commands.entity_predicate.effect.false_advanced_no_effect", displayName, effectName)));
        }
      }
    }
    if (result) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.effect.pass", displayName), attachments);
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.effect.fail", displayName), attachments);
    }
  }

  @Override
  public EntityPredicateType<EffectsEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EFFECTS;
  }

  @Override
  public String toOptionEntry() {
    final StringJoiner joiner = new StringJoiner(", ", "{", "}");
    for (var entry : effects) {
      joiner.add(entry.expressAsString());
    }

    return "effect=" + joiner;
  }

  public record Entry(Holder<MobEffect> effect, MobEffectsPredicate.MobEffectInstancePredicate data, boolean expected) implements ExpressionConvertible {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
        MobEffect.CODEC.fieldOf("effect").forGetter(Entry::effect),
        MobEffectsPredicate.MobEffectInstancePredicate.CODEC.fieldOf("data").forGetter(Entry::data),
        Codec.BOOL.optionalFieldOf("expected", true).forGetter(Entry::expected)
    ).apply(i, Entry::new));

    @Override
    public String expressAsString() {
      final Holder<MobEffect> effectEntry = effect;
      final MobEffectsPredicate.MobEffectInstancePredicate effectData = data;
      final StringJoiner joiner = new StringJoiner(", ", "{", "}");
      final MinMaxBounds.Ints amplifier = effectData.amplifier();
      boolean dummy = true;
      if (!amplifier.isAny()) {
        joiner.add("amplifier=" + StringUtil.wrapRange(amplifier));
        dummy = false;
      }
      final MinMaxBounds.Ints duration = effectData.duration();
      if (!duration.isAny()) {
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

      final String effectId = effectEntry.unwrap().map(key -> key.location().toString(), statusEffect -> MobEffect.CODEC.encodeStart(NbtOps.INSTANCE, effectEntry).getOrThrow().toString());
      if (dummy) {
        return effectId + "=" + expected;
      } else {
        return effectId + "=" + (expected ? "!" : "") + joiner;
      }
    }
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return Lists.transform(effects, Entry::effect);
  }
}
