package pers.solid.ecmd.function.property;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.function.SerializableFunction;
import pers.solid.ecmd.util.NbtConvertible;

/**
 * 用于修改一个方块的方块状态属性的函数，通常用于方块函数中。通常来说，状态属性函数包含状态属性以及一个值，用于将方块的这个属性设置为另一个值。
 *
 * @param <T> 该属性的类型。
 */
public interface PropertyFunction<T extends Comparable<T>> extends SerializableFunction, NbtConvertible {
  /**
   * 修改方块状态，并返回修改后的方块状态。由于方块状态是不可变对象，因此返回的是另一个方块状态对象（也有可能是同一个）。
   *
   * @param blockState 当前正在修改的方块状态
   * @param origState  整个修改过程之前的方块状态
   */
  @Contract(pure = true)
  BlockState getModifiedState(BlockState blockState, BlockState origState);

  /**
   * 该函数需要修改的那个属性，必须是准确的属性，而非根据属性的名称来匹配到那个名称的属性。
   */
  @Contract(pure = true)
  Property<T> property();

  static PropertyFunction<?> fromNbt(@NotNull NbtCompound nbtCompound, @NotNull Block block) {
    final String propertyName = nbtCompound.getString("property");
    final String valueName = nbtCompound.getString("value");
    final boolean must = nbtCompound.getBoolean("must");

    final Property<?> property = block.getStateManager().getProperty(propertyName);
    if (property == null) {
      throw new IllegalArgumentException("The block %s does not support property named '%s'.".formatted(block, propertyName));
    }
    if ("*".equals(valueName)) {
      return new RandomPropertyFunction<>(property, must);
    } else if ("~".equals(valueName)) {
      return new BypassingPropertyFunction<>(property, must);
    }
    return getSimplePropertyFunctionFromValue(property, valueName, must);
  }

  private static <T extends Comparable<T>> SimplePropertyFunction<T> getSimplePropertyFunctionFromValue(Property<T> property, String valueName, boolean must) {
    // 此方法独立出来是为了避免出现泛型错误。
    return new SimplePropertyFunction<>(property, property.parse(valueName).orElseThrow(() -> new IllegalArgumentException("The property '%s' does not support value named '%s'.".formatted(property.getName(), valueName))), must);
  }
}
