package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record LootTablePredicateAnonymousEntityPredicateEntry(LootCondition lootCondition, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (!(entity.world instanceof final ServerWorld serverWorld)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.predicate.not_on_server", displayName));
    } else {
      LootContext lootContext = new LootContext.Builder(serverWorld)
          .parameter(LootContextParameters.THIS_ENTITY, entity)
          .parameter(LootContextParameters.ORIGIN, entity.getPos())
          .build(LootContextTypes.SELECTOR);
      final boolean test = lootCondition.test(lootContext);
      if (hasNegation ^ test) {
        return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.predicate.pass_anonymous", displayName, TextUtil.literal(test).styled(TextUtil.STYLE_FOR_ACTUAL)));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.predicate.fail_anonymous", displayName, TextUtil.literal(test).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(!hasNegation).styled(TextUtil.STYLE_FOR_EXPECTED)));
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return "predicate=" + (hasNegation ? "!" : "") + EntitySelectorOptionsExtension.LOOT_CONDITION_GSON.toJson(lootCondition, LootCondition.class);
  }
}
