package pers.solid.ecmd.predicate.item;

import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.function.Predicate;

public interface ItemPredicateEntry extends ItemPredicate, Predicate<ItemStack> {
  @Override
  default boolean test(ItemStack stack, ExecutionContext executionContext) {
    return test(stack);
  }

  default String asEntryString() {
    return asString();
  }
}
