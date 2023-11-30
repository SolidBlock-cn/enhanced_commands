package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;

public record FireEntityPredicateEntry(NumberRange.IntRange intRange, boolean inverted) implements EntityPredicateEntry {
  public static final Text CRITERION_NAME = Text.translatable("enhanced_commands.argument.entity_predicate.fire");

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getFireTicks(), intRange, CRITERION_NAME, displayName, inverted);
  }

  @Override
  public String toOptionEntry() {
    return "fire=" + (inverted ? "!" : "") + StringUtil.wrapRange(intRange);
  }
}
