package pers.solid.ecmd.item.predicate;

import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.function.Predicate;

public interface ItemPredicateWithoutContext extends ItemPredicate, Predicate<ItemStack> {
  @Override
  default boolean test(ItemStack stack, ExecutionContext executionContext) {
    return test(stack);
  }
}
