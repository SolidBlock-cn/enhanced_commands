package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;

import java.util.function.Predicate;

public record SimpleBooleanEntityPredicateEntry(Predicate<Entity> predicate, boolean expected, String trueTranslationKey, String falseTranslationKey, String optionName) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    return predicate.test(entity) == expected;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final boolean test = predicate.test(entity);
    if (test) {
      return TestResult.of(expected, Text.translatable(trueTranslationKey, displayName));
    } else {
      return TestResult.of(!expected, Text.translatable(falseTranslationKey, displayName));
    }
  }

  @Override
  public String toOptionEntry() {
    return optionName + "=" + expected;
  }
}
