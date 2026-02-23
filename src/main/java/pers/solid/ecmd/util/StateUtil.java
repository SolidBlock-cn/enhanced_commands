package pers.solid.ecmd.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * 此类包含与方块状态有关的实用方法。
 */
public final class StateUtil {
  private StateUtil() {
  }

  /**
   * 获取方块状态的某个属性的值。这会调用 {@link Property#getName(Comparable)}，借助此方法以规避泛型问题。
   */
  public static <T extends Comparable<T>> String namePropertyValue(@NotNull StateHolder<?, ?> state, @NotNull Property<T> property) {
    return property.getName(state.getValue(property));
  }

  /**
   * 将方块状态的一个属性设置为随机的值。方块状态必须确实有这个属性。
   *
   * @throws IllegalArgumentException 如果方块状态没有这个。
   */
  public static <T extends Comparable<T>, S extends StateHolder<?, S>> @NotNull S withPropertyOfRandomValue(@NotNull S blockState, @NotNull Property<T> property, @NotNull RandomSource random) {
    final ImmutableList<T> values = ImmutableList.copyOf(property.getPossibleValues());
    return blockState.setValue(property, values.get(random.nextInt(values.size())));
  }

  /**
   * 将方块状态的所有属性均设置为随机的值。
   */
  public static @NotNull BlockState getBlockWithRandomProperties(@NotNull Block block, @NotNull RandomSource random) {
    final ImmutableList<BlockState> states = block.getStateDefinition().getPossibleStates();
    return states.get(random.nextInt(states.size()));
  }

  /**
   * 将方块状态的一个属性设置为由字符串决定的值。
   *
   * @param must 当方块状态的值不存在时，是否抛出错误。
   * @throws IllegalArgumentException 如果方块状态的值不存在，且 {@code must} 为 {@code true}，或者方块状态没有此属性。
   */
  public static <T extends Comparable<T>, S extends StateHolder<?, S>> @NotNull S withPropertyOfValueByName(@NotNull S state, @NotNull Property<T> property, @NotNull String valueName, boolean must) {
    final Optional<T> parse = property.getValue(valueName);
    if (parse.isEmpty()) {
      if (must) {
        throw new IllegalArgumentException("property probability");
      } else {
        return state;
      }
    }
    return state.setValue(property, parse.get());
  }

  /**
   * 将方块状态的一个属性的值设为另一个方块状态的此属性的值。
   */
  public static <T extends Comparable<T>, S extends StateHolder<?, S>> S withPropertyOfValueFromAnother(@NotNull S blockState, @NotNull S origState, @NotNull Property<T> property) {
    return blockState.setValue(property, origState.getValue(property));
  }
}
