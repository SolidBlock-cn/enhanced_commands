package pers.solid.ecmd.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

public record ValuePropertyPredicate<T extends Comparable<T>>(Property<T> property, Comparator comparator, T value) implements PropertyPredicate<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + comparator.asString() + property.name(value);
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) && comparator.test(blockState.get(property), value);
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("comparator", comparator.asString());
    nbtCompound.putString("value", property.name(value));
  }
}
