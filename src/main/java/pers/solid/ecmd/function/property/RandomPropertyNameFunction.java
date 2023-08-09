package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RandomPropertyNameFunction(String propertyName, boolean must) implements PropertyNameFunction {
  @Override
  public @NotNull String asString() {
    return propertyName + (must ? "==*" : "=*");
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
    final List<T> values = List.copyOf(property.getValues());
    return blockState.with(property, values.get(RandomUtils.nextInt(0, values.size())));
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("value", "*");
    nbtCompound.putBoolean("must", must);
  }
}
