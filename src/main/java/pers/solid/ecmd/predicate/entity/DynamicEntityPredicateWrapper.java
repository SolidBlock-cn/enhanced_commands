package pers.solid.ecmd.predicate.entity;

import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import org.apache.commons.lang3.mutable.MutableObject;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.function.Predicate;

/**
 * <p>包含一个需要指定 context 才能运作的 {@link EntityPredicate}（非 {@link StaticEntityPredicate}）的对象，并直接继承 {@link Predicate}，用于存储在 {@link EntitySelectorReader#predicates} 和 {@link EntitySelector#predicates}中。
 * <p>{@link #contextWrapper} 在执行过程中是可能会改变的。
 */
public record DynamicEntityPredicateWrapper(EntityPredicate entityPredicate, MutableObject<ExecutionContext> contextWrapper) implements Predicate<Entity> {
  @Override
  public boolean test(Entity entity) {
    return entityPredicate().test(entity, contextWrapper.getValue());
  }
}
