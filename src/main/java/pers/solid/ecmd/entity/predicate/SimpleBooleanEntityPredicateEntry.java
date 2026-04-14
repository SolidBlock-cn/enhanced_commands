package pers.solid.ecmd.entity.predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record SimpleBooleanEntityPredicateEntry(SimpleBooleanEntityPredicateType type, boolean expected) implements EntityPredicateEntry, StaticEntityPredicate {
  @Override
  public boolean test(Entity entity) {
    return type.predicate.test(entity) == expected;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final boolean test = type.predicate.test(entity);
    if (test) {
      return TestResult.of(expected, Component.translatable(type.trueTranslationKey, displayName));
    } else {
      return TestResult.of(!expected, Component.translatable(type.falseTranslationKey, displayName));
    }
  }

  @Override
  public EntityPredicateType<SimpleBooleanEntityPredicateEntry> getType() {
    return type;
  }

  @Override
  public String toOptionEntry() {
    return type.optionName + "=" + expected;
  }
}
