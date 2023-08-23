package pers.solid.ecmd.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

public record ExistencePropertyPredicate<T extends Comparable<T>>(Property<T> property, boolean exists) implements PropertyPredicate<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + (exists ? "=*" : "!=*");
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) == exists;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putBoolean("exists", exists);
  }
}
