package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;

import java.util.function.Predicate;

/**
 * 这里是指由 {@link net.minecraft.command.EntitySelectorOptions} 指定，但是没有通过本模组指定序列化方块的实体谓词。这种情况下，会无法序列化。
 */
public record UnknownEntityPredicateEntry(Predicate<Entity> predicate) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    return predicate.test(entity);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (test(entity)) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.unknown.true", displayName));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.unknown.false", displayName));
    }
  }

  @Override
  public @Nullable String toOptionEntry() {
    return "/* unknown */";
  }
}
