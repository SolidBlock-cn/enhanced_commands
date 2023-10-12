package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

public record SimplePropertyNameFunction(String propertyName, String valueName, boolean must) implements PropertyNameFunction {
  @Override
  public @NotNull String asString() {
    return propertyName + (must ? "==" : "=") + valueName;
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return StateUtil.withPropertyOfValueByName(blockState, property, valueName, must);
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("value", valueName);
    nbtCompound.putBoolean("must", must);
  }
}
