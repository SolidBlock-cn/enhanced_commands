package pers.solid.ecmd.entity.predicate;

import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.function.Predicate;

/**
 * 此类为不需要借助 {@link ExecutionContext} 就能运作的实体谓词，将能直接继承 {@link Predicate#test(Object)} 并存储于 {@link EntitySelector#contextFreePredicates} 中。
 */
public interface StaticEntityPredicate extends EntityPredicate, Predicate<Entity> {
  @Override
  default boolean test(Entity entity, ExecutionContext context) {
    return test(entity);
  }
}
