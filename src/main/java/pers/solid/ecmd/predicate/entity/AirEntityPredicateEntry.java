package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;

public record AirEntityPredicateEntry(NumberRange.IntRange intRange, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    return intRange.test(entity.getAir()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getAir(), intRange, Text.translatable("enhanced_commands.argument.entity_predicate.air"), displayName, inverted);
  }

  @Override
  public String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + StringUtil.wrapRange(intRange);
  }
}
