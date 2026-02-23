package pers.solid.ecmd.predicate.entity;

import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

/**
 * <p>可用于包装原版的 {@code Predicate<Entity>}、存储于原版的 {@link net.minecraft.commands.arguments.selector.EntitySelector#predicates} 并借此包装一个本模组的 {@link EntityPredicate} 对象的类。其行为将于原版的 {@link #vanillaPredicate} 一致，不过在进行反序列化以及谓词时，会使用到 {@link EntityPredicate} 中的信息。
 * <p>请注意，此类并不直接继承 {@link EntityPredicate}。
 *
 * @param vanillaPredicate 原版的谓词
 * @param entityPredicate  本模组中的实体谓词
 */
public record StaticEntityPredicateWrapper(Predicate<Entity> vanillaPredicate, StaticEntityPredicate entityPredicate) implements Predicate<Entity> {
  @Override
  public boolean test(Entity entity) {
    return vanillaPredicate.test(entity);
  }
}
