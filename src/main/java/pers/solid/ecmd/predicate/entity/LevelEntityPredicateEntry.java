package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;

public record LevelEntityPredicateEntry(NumberRange.IntRange intRange, boolean inverted) implements EntityPredicateEntry {
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.argument.entity_predicate.level");

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.general.not_player", CRITERION_NAME, displayName));
    } else {
      return EntityPredicateEntry.testInt(player, player.experienceLevel, intRange, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public String toOptionEntry() {
    return "level=" + (inverted ? "!" : "") + StringUtil.wrapRange(intRange);
  }
}
