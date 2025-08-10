package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.function.Predicate;

/**
 * 这里是指由 {@link net.minecraft.command.EntitySelectorOptions} 指定，但是没有通过本模组指定序列化方块的实体谓词。这种情况下，会无法序列化。
 */
public record UnknownEntityPredicateEntry(Predicate<Entity> predicate) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<UnknownEntityPredicateEntry> UNKNOWN = MapCodec.unit(new UnknownEntityPredicateEntry(Predicates.alwaysTrue()));

  @Override
  public boolean test(@NotNull Entity entity) {
    return predicate.test(entity);
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    if (test(entity, context)) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.unknown.true", displayName));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.unknown.false", displayName));
    }
  }

  @Override
  public @NotNull EntityPredicateType<UnknownEntityPredicateEntry> getType() {
    return EntityPredicateTypes.UNKNOWN;
  }

  @Override
  public String toOptionEntry() {
    return "/* unknown */";
  }
}
