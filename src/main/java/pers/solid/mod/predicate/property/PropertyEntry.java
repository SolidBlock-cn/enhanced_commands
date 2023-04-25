package pers.solid.mod.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import pers.solid.mod.predicate.SerializablePredicate;

public interface PropertyEntry<T extends Comparable<T>> extends SerializablePredicate {
  boolean test(BlockState blockState);

  Property<T> property();
}
