package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.LootBridge;

import java.util.Optional;

public record LootTablePredicateEntityPredicateEntry(@NotNull RegistryEntry<LootCondition> predicate, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<LootTablePredicateEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      LootCondition.ENTRY_CODEC.fieldOf("predicate").forGetter(LootTablePredicateEntityPredicateEntry::predicate),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(LootTablePredicateEntityPredicateEntry::inverted)
  ).apply(i, LootTablePredicateEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity.getWorld() instanceof final ServerWorld serverWorld)) {
      return false;
    } else {
      final LootCondition lootCondition;
      if (predicate instanceof RegistryEntry.Reference<LootCondition> reference) {
        final Optional<LootCondition> optional = LootBridge.getLootCondition(serverWorld.getServer(), reference.registryKey());
        if (optional.isEmpty()) {
          return false;
        }
        lootCondition = optional.get();
      } else {
        lootCondition = predicate.value();
      }
      LootContext lootContext = new LootContext.Builder(new LootContextParameterSet.Builder(serverWorld)
          .add(LootContextParameters.THIS_ENTITY, entity)
          .add(LootContextParameters.ORIGIN, entity.getPos())
          .build(LootContextTypes.SELECTOR)).build(Optional.empty());
      final boolean test = lootCondition.test(lootContext);
      return inverted ^ test;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    if (!(entity.getWorld() instanceof final ServerWorld serverWorld)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.not_on_server", displayName));
    } else {
      final LootCondition lootCondition;
      if (predicate instanceof RegistryEntry.Reference<LootCondition> reference) {
        final Optional<LootCondition> optional = LootBridge.getLootCondition(serverWorld.getServer(), reference.registryKey());
        if (optional.isEmpty()) {
          return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.unknown_predicate", TextUtil.literal(reference.registryKey().getValue()).styled(Styles.TARGET)));
        }
        lootCondition = optional.get();
        LootContext lootContext = new LootContext.Builder(new LootContextParameterSet.Builder(serverWorld)
            .add(LootContextParameters.THIS_ENTITY, entity)
            .add(LootContextParameters.ORIGIN, entity.getPos())
            .build(LootContextTypes.SELECTOR)).build(Optional.empty());
        final boolean test = lootCondition.test(lootContext);
        if (inverted ^ test) {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.predicate.pass", displayName, TextUtil.literal(reference.registryKey().getValue()).styled(Styles.TARGET), TextUtil.literal(test).styled(Styles.ACTUAL)));
        } else {
          return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.fail", displayName, TextUtil.literal(reference.registryKey().getValue()).styled(Styles.TARGET), TextUtil.literal(test).styled(Styles.ACTUAL), TextUtil.literal(!inverted).styled(Styles.EXPECTED)));
        }
      } else {
        lootCondition = predicate.value();
        LootContext lootContext = new LootContext.Builder(new LootContextParameterSet.Builder(serverWorld)
            .add(LootContextParameters.THIS_ENTITY, entity)
            .add(LootContextParameters.ORIGIN, entity.getPos())
            .build(LootContextTypes.SELECTOR))
            .build(Optional.empty());
        final boolean test = lootCondition.test(lootContext);
        if (inverted ^ test) {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.predicate.pass_anonymous", displayName, TextUtil.literal(test).styled(Styles.ACTUAL)));
        } else {
          return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.fail_anonymous", displayName, TextUtil.literal(test).styled(Styles.ACTUAL), TextUtil.literal(!inverted).styled(Styles.EXPECTED)));
        }
      }
    }
  }

  @Override
  public @NotNull EntityPredicateType<LootTablePredicateEntityPredicateEntry> getType() {
    return EntityPredicateTypes.LOOT_TABLE_PREDICATE;
  }

  @Override
  public String toOptionEntry() {
    return "predicate=" + (inverted ? "!" : "") + predicate.getKey().map(registryKey -> registryKey.getValue().toString()).orElseGet(() -> LootCondition.CODEC.encodeStart(JsonOps.INSTANCE, predicate.value()).getOrThrow().toString());
  }
}
