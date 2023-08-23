package pers.solid.ecmd.function.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

/**
 * 此属性函数用于表示不改变方块状态属性，也就是说改变方块之后仍保留原来的方块状态属性的值。例如：
 * <pre>
 *   spruce_stairs[facing=~](oak_stairs[facing=east]) = oak_stairs[facing=west]
 * </pre>
 * {@code must} 参数用于控制当没有指定的方块状态属性时的行为。如果为 true，当不存在有关的方块状态属性时，抛出错误。如果为 false，则不执行。
 * <pre>
 *   spruce_stairs[facing=~](dirt) = spruce_stairs
 *   spruce_stairs[facing==~](dirt) = IllegalArgumentException
 * </pre>
 */
public record BypassingPropertyFunction<T extends Comparable<T>>(Property<T> property, boolean must) implements PropertyFunction<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==~" : "=~");
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState) {
    if (must || (blockState.contains(property) && origState.contains(property))) {
      return blockState.with(property, origState.get(property));
    } else {
      return blockState;
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("value", "~");
    nbtCompound.putBoolean("must", must);
  }
}
