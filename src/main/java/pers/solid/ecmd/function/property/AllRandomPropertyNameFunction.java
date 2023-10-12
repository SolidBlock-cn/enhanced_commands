package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

import java.util.Collection;

public record AllRandomPropertyNameFunction(@NotNull Collection<String> except) implements GeneralPropertyFunction.OfName {
  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
    if (except.isEmpty()) {
      return StateUtil.getBlockWithRandomProperties(blockState.getBlock(), random);
    } else {
      for (Property<?> property : blockState.getProperties()) {
        if (!except.contains(property.getName())) {
          blockState = StateUtil.withPropertyOfRandomValue(blockState, property, random);
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
