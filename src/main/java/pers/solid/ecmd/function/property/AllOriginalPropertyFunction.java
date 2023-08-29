package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

import java.util.Collection;

public record AllOriginalPropertyFunction(@NotNull Collection<Property<?>> except) implements GeneralPropertyFunction {
  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    for (Property<?> property : blockState.getProperties()) {
      if (!except.contains(property) && origState.contains(property)) {
        blockState = StateUtil.withPropertyOfValueFromAnother(blockState, origState, property);
      }
    }
    return blockState;
  }

  @Override
  public Property<Integer> property() {
    return null;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", "~");
  }
}
