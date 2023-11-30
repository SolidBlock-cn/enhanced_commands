package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;

public record FoodEntityPredicateEntry(NumberRange.IntRange intRange, boolean inverted) implements EntityPredicateEntry {
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.argument.entity_predicate.food");

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testInt(player, player.getHungerManager().getFoodLevel(), intRange, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public String toOptionEntry() {
    return "food=" + (inverted ? "!" : "") + StringUtil.wrapRange(intRange);
  }
}
