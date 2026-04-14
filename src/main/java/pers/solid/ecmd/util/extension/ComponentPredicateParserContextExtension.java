package pers.solid.ecmd.util.extension;

import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.core.HolderLookup;
import pers.solid.ecmd.item.predicate.ItemPredicate;

import java.util.List;

/**
 * 用于扩展 {@link ComponentPredicateParser}。
 */
public interface ComponentPredicateParserContextExtension<T> {
  default T allOf$enhanced_commands(List<T> values) {
    if (values.size() == 1) {
      return values.get(0);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  default T combine$enhanced_commands(List<T> values) {
    return allOf$enhanced_commands(values);
  }

  default boolean supportsItemPredicate$enhanced_commands() {
    return false;
  }

  default T convertFromItemPredicate$enhanced_commands(ItemPredicate itemPredicate) {
    throw new UnsupportedOperationException();
  }

  default HolderLookup.Provider registries$enhanced_commands() {
    throw new UnsupportedOperationException();
  }
}
