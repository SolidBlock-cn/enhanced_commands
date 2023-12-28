package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record LootTablePredicateEntityPredicateEntry(Identifier predicateId, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity.world instanceof final ServerWorld serverWorld)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.not_on_server", displayName));
    } else {
      LootCondition lootCondition = serverWorld.getServer().getPredicateManager().get(predicateId);
      if (lootCondition == null) {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.unknown_predicate", TextUtil.literal(predicateId).styled(TextUtil.STYLE_FOR_TARGET)));
      } else {
        LootContext lootContext = new LootContext.Builder(serverWorld)
            .parameter(LootContextParameters.THIS_ENTITY, entity)
            .parameter(LootContextParameters.ORIGIN, entity.getPos())
            .build(LootContextTypes.SELECTOR);
        final boolean test = lootCondition.test(lootContext);
        if (hasNegation ^ test) {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.predicate.pass", displayName, TextUtil.literal(predicateId).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(test).styled(TextUtil.STYLE_FOR_ACTUAL)));
        } else {
          return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.fail", displayName, TextUtil.literal(predicateId).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(test).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(!hasNegation).styled(TextUtil.STYLE_FOR_EXPECTED)));
        }
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return null;
  }
}
