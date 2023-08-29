package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

import java.util.Collection;

public record AllRandomPropertyNameFunction(@NotNull Collection<String> except) implements GeneralPropertyFunction.OfName {
  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    if (except.isEmpty()) {
      return StateUtil.getBlockWithRandomProperties(blockState.getBlock(), AllRandomPropertyFunction.RANDOM);
    } else {
      for (Property<?> property : blockState.getProperties()) {
        if (!except.contains(property.getName())) {
          blockState = StateUtil.withPropertyOfRandomValue(blockState, property, AllRandomPropertyFunction.RANDOM);
        }
      }
      return blockState;
    }
  }

  @Override
  public String propertyName() {
    return null;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", "*");
  }
}
