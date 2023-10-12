package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

import java.util.Collection;

public record AllOriginalPropertyNameFunctions(@NotNull Collection<String> except) implements GeneralPropertyFunction.OfName {
  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
    for (Property<?> property : blockState.getProperties()) {
      if (!except.contains(property.getName()) && origState.contains(property)) {
        blockState = StateUtil.withPropertyOfValueFromAnother(blockState, origState, property);
      }
    }
    return blockState;
  }

  @Override
  public String propertyName() {
    return null;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", "~");
  }
}
