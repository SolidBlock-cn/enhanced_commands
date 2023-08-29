package pers.solid.ecmd.function.property;

import com.google.common.collect.Collections2;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

import java.util.Collection;

public record AllRandomPropertyFunction(@NotNull Collection<Property<?>> except) implements GeneralPropertyFunction {
  static final Random RANDOM = Random.create();

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    if (except.isEmpty()) {
      return StateUtil.getBlockWithRandomProperties(blockState.getBlock(), RANDOM);
    } else {
      for (Property<?> property : Collections2.filter(blockState.getProperties(), predicate -> !except.contains(predicate))) {
        blockState = StateUtil.withPropertyOfRandomValue(blockState, property, RANDOM);
      }
      return blockState;
    }
  }

  @Override
  public Property<Integer> property() {
    return null;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", "*");
  }
}
