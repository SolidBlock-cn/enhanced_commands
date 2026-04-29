package pers.solid.ecmd.property.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public record SimplePropertyNameFunction(String propertyName, String valueName, boolean must) implements PropertyNameFunction {
  public static final MapCodec<SimplePropertyNameFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(
      SimplePropertyNameFunction::new,
      Codec.STRING.fieldOf("property").forGetter(SimplePropertyNameFunction::propertyName),
      Codec.STRING.fieldOf("value").forGetter(SimplePropertyNameFunction::valueName),
      Codec.BOOL.optionalFieldOf("must", false).forGetter(SimplePropertyNameFunction::must)
  ));
  public static final Dynamic2CommandExceptionType PROPERTY_DOES_NOT_SUPPORT_VALUE = new Dynamic2CommandExceptionType((propertyName, valueName) -> Component.translatable("enhanced_commands.property_function.property_does_not_support_value", propertyName, valueName));

  /**
   * 将方块状态的一个属性设置为由字符串决定的值。
   *
   * @param must 当方块状态的值不存在时，是否抛出错误。
   * @throws IllegalArgumentException 如果方块状态的值不存在，且 {@code must} 为 {@code true}，或者方块状态没有此属性。
   */
  public static <T extends Comparable<T>, S extends StateHolder<?, S>> S withPropertyOfValueByName(S state, Property<T> property, String valueName, boolean must) throws CommandSyntaxException {
    final Optional<T> parse = property.getValue(valueName);
    if (parse.isEmpty()) {
      if (must) {
        throw PROPERTY_DOES_NOT_SUPPORT_VALUE.create(property.getName(), valueName);
      } else {
        return state;
      }
    }
    return state.setValue(property, parse.get());
  }

  @Override
  public String expressAsString() {
    return propertyName + (must ? "==" : "=") + valueName;
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, RandomSource random) throws CommandSyntaxException {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return withPropertyOfValueByName(blockState, property, valueName, must);
  }

  @Override
  public Type getType() {
    return Type.SIMPLE;
  }

}
