package pers.solid.ecmd.function.property;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.StringRepresentableFunction;
import pers.solid.ecmd.util.NbtConvertible;

public interface PropertyNameFunction extends StringRepresentableFunction, NbtConvertible {
  static PropertyNameFunction fromNbt(NbtCompound nbtCompound) {
    final String property = nbtCompound.getString("property");
    final String value = nbtCompound.getString("value");
    final boolean must = nbtCompound.getBoolean("must");
    if ("~".equals(value)) {
      return new BypassingPropertyNameFunction(property, must);
    } else if ("*".equals(value)) {
      return new RandomPropertyNameFunction(property, must);
    } else {
      return new SimplePropertyNameFunction(property, value, must);
    }
  }

  @Contract(pure = true)
  BlockState getModifiedState(BlockState blockState, BlockState origState);

  @Contract(pure = true)
  String propertyName();

  /**
   * 当 must 为 true 时，返回属性或者抛出异常。当 must 为 false 时，返回属性或者 null，不抛出异常。
   */
  @Nullable
  static Property<?> getProperty(@NotNull BlockState blockState, String propertyName, boolean must) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null || !blockState.contains(property)) {
      if (must) {
        throw new IllegalArgumentException("property name");
      } else {
        return null;
      }
    }
    return property;
  }
}
