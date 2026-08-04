package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.LootBridge;

import java.util.List;
import java.util.Optional;

public record LootTablePredicateEntityPredicateEntry(Holder<LootItemCondition> predicate, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<LootTablePredicateEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      LootItemCondition.CODEC.fieldOf("predicate").forGetter(LootTablePredicateEntityPredicateEntry::predicate),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(LootTablePredicateEntityPredicateEntry::inverted)
  ).apply(i, LootTablePredicateEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    if (!(entity.level() instanceof final ServerLevel serverWorld)) {
      return false;
    } else {
      final LootItemCondition lootCondition;
      if (predicate instanceof Holder.Reference<LootItemCondition> reference) {
        final Optional<LootItemCondition> optional = LootBridge.getLootCondition(serverWorld.getServer(), reference.key());
        if (optional.isEmpty()) {
          return false;
        }
        lootCondition = optional.get();
      } else {
        lootCondition = predicate.value();
      }
      LootContext lootContext = new LootContext.Builder(new LootParams.Builder(serverWorld)
          .withParameter(LootContextParams.THIS_ENTITY, entity)
          .withParameter(LootContextParams.ORIGIN, entity.position())
          .create(LootContextParamSets.SELECTOR)).create(Optional.empty());
      final boolean test = lootCondition.test(lootContext);
      return inverted ^ test;
    }
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity.level() instanceof final ServerLevel serverWorld)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.predicate.not_on_server", displayName));
    } else {
      final LootItemCondition lootCondition;
      if (predicate instanceof Holder.Reference<LootItemCondition> reference) {
        final Optional<LootItemCondition> optional = LootBridge.getLootCondition(serverWorld.getServer(), reference.key());
        if (optional.isEmpty()) {
          return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.predicate.unknown_predicate", TextUtil.literal(reference.key().location()).withStyle(Styles.TARGET)));
        }
        lootCondition = optional.get();
        LootContext lootContext = new LootContext.Builder(new LootParams.Builder(serverWorld)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, entity.position())
            .create(LootContextParamSets.SELECTOR)).create(Optional.empty());
        final boolean test = lootCondition.test(lootContext);
        if (inverted ^ test) {
          return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.predicate.pass", displayName, TextUtil.literal(reference.key().location()).withStyle(Styles.TARGET), TextUtil.literal(test).withStyle(Styles.ACTUAL)));
        } else {
          return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.predicate.fail", displayName, TextUtil.literal(reference.key().location()).withStyle(Styles.TARGET), TextUtil.literal(test).withStyle(Styles.ACTUAL), TextUtil.literal(!inverted).withStyle(Styles.EXPECTED)));
        }
      } else {
        lootCondition = predicate.value();
        LootContext lootContext = new LootContext.Builder(new LootParams.Builder(serverWorld)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, entity.position())
            .create(LootContextParamSets.SELECTOR))
            .create(Optional.empty());
        final boolean test = lootCondition.test(lootContext);
        if (inverted ^ test) {
          return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.predicate.pass_anonymous", displayName, TextUtil.literal(test).withStyle(Styles.ACTUAL)));
        } else {
          return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.predicate.fail_anonymous", displayName, TextUtil.literal(test).withStyle(Styles.ACTUAL), TextUtil.literal(!inverted).withStyle(Styles.EXPECTED)));
        }
      }
    }
  }

  @Override
  public EntityPredicateType<LootTablePredicateEntityPredicateEntry> getType() {
    return EntityPredicateTypes.LOOT_TABLE_PREDICATE;
  }

  @Override
  public String toOptionEntry() {
    return "predicate=" + (inverted ? "!" : "") + predicate.unwrapKey().map(registryKey -> registryKey.location().toString()).orElseGet(() -> LootItemCondition.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, predicate.value()).getOrThrow().toString());
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(predicate);
  }
}
