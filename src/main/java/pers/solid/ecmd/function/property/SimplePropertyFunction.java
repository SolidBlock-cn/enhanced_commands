package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

/**
 * 简单地设置将方块的某个属性设置为值。其中，{@code must} 参考用于控制方块状态不存在时，是直接失败还是不执行。但由于命令解析时就已经判断好了方块状态是否存在，因此此参数的作用不大。
 *
 * @param must 指定方块状态不存在时的行为。
 */
public record SimplePropertyFunction<T extends Comparable<T>>(Property<T> property, T value, boolean must) implements PropertyFunction<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==" : "=") + property.name(value);
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    if (must || blockState.contains(property)) {
      return blockState.with(property, value);
    } else {
      return blockState;
    }
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("value", property.name(value));
    nbtCompound.putBoolean("must", must);
  }
}
