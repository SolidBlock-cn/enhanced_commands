package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record SimplePropertyNameFunction(String propertyName, String valueName, boolean must) implements PropertyNameFunction {
  @Override
  public @NotNull String asString() {
    return propertyName + (must ? "==" : "=") + valueName;
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return getModifiedStateForProperty(blockState, property);
  }

  private <T extends Comparable<T>> BlockState getModifiedStateForProperty(BlockState blockState, Property<T> property) {
    final Optional<T> parse = property.parse(valueName);
    if (parse.isEmpty()) {
      if (must) {
        throw new IllegalArgumentException("property value");
      } else {
        return blockState;
      }
    }
    return blockState.with(property, parse.get());
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("value", valueName);
    nbtCompound.putBoolean("must", must);
  }
}
