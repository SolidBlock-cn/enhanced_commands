package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

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
    return StateUtil.withPropertyOfValueFromAnother(blockState, origState, property);
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("value", "~");
    nbtCompound.putBoolean("must", must);
  }
}
