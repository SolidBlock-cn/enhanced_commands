package pers.solid.ecmd.mixins.ext;

import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.mixins.mixin.EntitySelectorParserMixin;
import pers.solid.ecmd.predicate.entity.EntityPredicateEntry;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;
import pers.solid.ecmd.predicate.entity.StaticEntityPredicate;

import java.util.function.Predicate;

/**
 * 此接口通过 {@link EntitySelectorParserMixin} 使得 {@link EntitySelectorParser} 实现。
 */
public interface EntitySelectorParserExtension {
  /**
   * 获取原版的 {@link EntitySelectorParser} 对象中，通过本模组加入的包含一些扩展信息的对象的字段。
   */
  @Contract(pure = true)
  default EntitySelectorReaderExtras extension$ec() {
    throw new UnsupportedOperationException();
  }

  default void addPredicate(EntityPredicateEntry predicateEntry) {
    extension$ec().addPredicate(predicateEntry);
  }

  /**
   * 既满足原版的 {@link EntitySelectorParser#addPredicate(Predicate)}，又满足 {@link #addPredicate(EntityPredicateEntry)} 时，优先添加至原版的相关字段中，即添加至 {@link EntitySelectorParser#predicates}。
   *
   * @see StaticEntityPredicate
   */
  default <T extends Predicate<Entity> & EntityPredicateEntry> void addPredicate(T predicateEntry) {
    ((EntitySelectorParser) this).addPredicate(((Predicate<Entity>) predicateEntry));
  }
}
