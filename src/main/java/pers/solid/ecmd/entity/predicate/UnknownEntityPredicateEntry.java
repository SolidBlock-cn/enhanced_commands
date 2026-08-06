package pers.solid.ecmd.entity.predicate;

import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.function.Predicate;

/**
 * 这里是指由 {@link net.minecraft.commands.arguments.selector.options.EntitySelectorOptions} 指定，但是没有通过本模组指定序列化方块的实体谓词。这种情况下，会无法序列化。
 */
public record UnknownEntityPredicateEntry(Predicate<Entity> predicate) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<UnknownEntityPredicateEntry> UNKNOWN = MapCodec.unit(new UnknownEntityPredicateEntry(Predicates.alwaysTrue()));

  @Override
  public boolean test(Entity entity) {
    return predicate.test(entity);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (test(entity, context)) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.unknown.true", displayName));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.unknown.false", displayName));
    }
  }

  @Override
  public EntityPredicateType<UnknownEntityPredicateEntry> getType() {
    return EntityPredicateTypes.UNKNOWN;
  }

  @Override
  public String toOptionEntry() {
    return "/* unknown */";
  }
}
