package pers.solid.ecmd.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

public record ValuePropertyEntry<T extends Comparable<T>>(Property<T> property, Comparator comparator, T value) implements PropertyEntry<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + comparator.asString() + property.name(value);
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) && comparator.test(blockState.get(property), value);
  }
}
