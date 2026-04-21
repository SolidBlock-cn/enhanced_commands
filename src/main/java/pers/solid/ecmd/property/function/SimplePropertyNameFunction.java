package pers.solid.ecmd.property.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import pers.solid.ecmd.util.StateUtil;

public record SimplePropertyNameFunction(String propertyName, String valueName, boolean must) implements PropertyNameFunction {
  public static final MapCodec<SimplePropertyNameFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(
      SimplePropertyNameFunction::new,
      Codec.STRING.fieldOf("property").forGetter(SimplePropertyNameFunction::propertyName),
      Codec.STRING.fieldOf("value").forGetter(SimplePropertyNameFunction::valueName),
      Codec.BOOL.optionalFieldOf("must", false).forGetter(SimplePropertyNameFunction::must)
  ));

  @Override
  public String expressAsString() {
    return propertyName + (must ? "==" : "=") + valueName;
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, RandomSource random) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return StateUtil.withPropertyOfValueByName(blockState, property, valueName, must); // 考虑抛出 CommandSyntaxException
  }

  @Override
  public Type getType() {
    return Type.SIMPLE;
  }

}
