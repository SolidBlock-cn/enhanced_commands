package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.LootBridge;

import java.util.Optional;

public record LootTablePredicateEntityPredicateEntry(Identifier predicateId, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity.getWorld() instanceof final ServerWorld serverWorld)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.not_on_server", displayName));
    } else {
      Optional<LootCondition> lootCondition = LootBridge.getLootCondition(serverWorld, predicateId);
      if (lootCondition.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.unknown_predicate", TextUtil.literal(predicateId).styled(Styles.TARGET)));
      } else {
        LootContext lootContext = new LootContext.Builder(new LootContextParameterSet.Builder(serverWorld)
            .add(LootContextParameters.THIS_ENTITY, entity)
            .add(LootContextParameters.ORIGIN, entity.getPos())
            .build(LootContextTypes.SELECTOR)).build(Optional.empty());
        final boolean test = lootCondition.get().test(lootContext);
        if (hasNegation ^ test) {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.predicate.pass", displayName, TextUtil.literal(predicateId).styled(Styles.TARGET), TextUtil.literal(test).styled(Styles.ACTUAL)));
        } else {
          return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.fail", displayName, TextUtil.literal(predicateId).styled(Styles.TARGET), TextUtil.literal(test).styled(Styles.ACTUAL), TextUtil.literal(!hasNegation).styled(Styles.EXPECTED)));
        }
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return null;
  }
}
