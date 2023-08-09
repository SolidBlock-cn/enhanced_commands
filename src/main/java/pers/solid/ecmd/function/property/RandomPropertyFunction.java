package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 给予一个随机的方块状态属性。
 */
public record RandomPropertyFunction<T extends Comparable<T>>(Property<T> property, boolean must) implements PropertyFunction<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==*" : "=*");
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    if (must || blockState.contains(property)) {
      final List<T> values = List.copyOf(property.getValues());
      return blockState.with(property, values.get(RandomUtils.nextInt(0, values.size())));
    } else {
      return blockState;
    }
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("value", "*");
    nbtCompound.putBoolean("must", must);
  }
}
