package pers.solid.mod.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

public record ExistencePropertyEntry<T extends Comparable<T>>(Property<T> property, boolean exists) implements PropertyEntry<T> {
  @Override
  public String asString() {
    return property.getName() + (exists ? "=*" : "!=*");
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) == exists;
  }
}
