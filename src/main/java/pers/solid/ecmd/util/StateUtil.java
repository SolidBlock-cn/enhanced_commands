package pers.solid.ecmd.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 此类包含与方块状态有关的实用方法。
 */
public final class StateUtil {

  private StateUtil() {
  }

  /**
   * 获取方块状态的某个属性的值。这会调用 {@link Property#getName(Comparable)}，借助此方法以规避泛型问题。
   */
  public static <T extends Comparable<T>> String namePropertyValue(StateHolder<?, ?> state, Property<T> property) {
    return property.getName(state.getValue(property));
  }

  /**
   * 将方块状态的一个属性设置为随机的值。方块状态必须确实有这个属性。
   *
   * @throws IllegalArgumentException 如果方块状态没有这个。
   */
  public static <T extends Comparable<T>, S extends StateHolder<?, S>> S withPropertyOfRandomValue(S blockState, Property<T> property, RandomSource random) {
    final ImmutableList<T> values = ImmutableList.copyOf(property.getPossibleValues());
    return blockState.setValue(property, values.get(random.nextInt(values.size())));
  }

  /**
   * 将方块状态的所有属性均设置为随机的值。
   */
  public static BlockState getBlockWithRandomProperties(Block block, RandomSource random) {
    final ImmutableList<BlockState> states = block.getStateDefinition().getPossibleStates();
    return states.get(random.nextInt(states.size()));
  }

  /**
   * 将方块状态的一个属性的值设为另一个方块状态的此属性的值。
   */
  public static <T extends Comparable<T>, S extends StateHolder<?, S>> S withPropertyOfValueFromAnother(S blockState, S origState, Property<T> property) {
    return blockState.setValue(property, origState.getValue(property));
  }
}
