package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

public record LevelEntityPredicateEntry(NumberRange.IntRange intRange) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.level.not_player", displayName));
    } else {
      final boolean inRange = intRange.test(player.experienceLevel);
      return TestResult.of(inRange, Text.translatable("enhanced_commands.argument.entity_predicate.level." + inRange, displayName, TextUtil.literal(player.experienceLevel).styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal(StringUtil.wrapRange(intRange)).styled(TextUtil.STYLE_FOR_EXPECTED)));
    }
  }

  @Override
  public String toOptionEntry() {
    return "level=" + StringUtil.wrapRange(intRange);
  }
}
