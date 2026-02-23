package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record SimpleBooleanEntityPredicateEntry(SimpleBooleanEntityPredicateType type, boolean expected) implements EntityPredicateEntry, StaticEntityPredicate {
  @Override
  public boolean test(@NotNull Entity entity) {
    return type.predicate.test(entity) == expected;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    final boolean test = type.predicate.test(entity);
    if (test) {
      return TestResult.of(expected, Component.translatable(type.trueTranslationKey, displayName));
    } else {
      return TestResult.of(!expected, Component.translatable(type.falseTranslationKey, displayName));
    }
  }

  @Override
  public @NotNull EntityPredicateType<SimpleBooleanEntityPredicateEntry> getType() {
    return type;
  }

  @Override
  public String toOptionEntry() {
    return type.optionName + "=" + expected;
  }
}
