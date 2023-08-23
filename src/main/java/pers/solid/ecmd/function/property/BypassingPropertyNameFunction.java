package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

public record BypassingPropertyNameFunction(String propertyName, boolean must) implements PropertyNameFunction {
  @Override
  public @NotNull String asString() {
    return propertyName + (must ? "==~" : "=~");
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return getModifiedStateForProperty(blockState, origState, property);
  }

  private <T extends Comparable<T>> BlockState getModifiedStateForProperty(BlockState blockState, BlockState origState, Property<T> property) {
    return blockState.with(property, origState.get(property));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("value", "~");
    nbtCompound.putBoolean("must", must);
  }
}
