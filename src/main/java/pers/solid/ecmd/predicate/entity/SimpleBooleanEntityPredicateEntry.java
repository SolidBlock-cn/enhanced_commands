package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;

import java.util.function.Predicate;

public record SimpleBooleanEntityPredicateEntry(Predicate<Entity> predicate, boolean expected, String trueTranslationKey, String falseTranslationKey, String optionName) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
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
  public @Nullable String toOptionEntry() {
    return optionName + "=" + expected;
  }
}
