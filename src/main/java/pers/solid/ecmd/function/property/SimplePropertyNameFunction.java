package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

public record SimplePropertyNameFunction(String propertyName, String valueName, boolean must) implements PropertyNameFunction {
  public static final Codec<SimplePropertyNameFunction> CODEC = RecordCodecBuilder.create(i -> i.apply3(
      SimplePropertyNameFunction::new,
      Codec.STRING.fieldOf("property").forGetter(SimplePropertyNameFunction::propertyName),
      Codec.STRING.fieldOf("value").forGetter(SimplePropertyNameFunction::valueName),
      Codec.BOOL.optionalFieldOf("must", false).forGetter(SimplePropertyNameFunction::must)
  ));

  @Override
  public @NotNull String asString() {
    return propertyName + (must ? "==" : "=") + valueName;
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return StateUtil.withPropertyOfValueByName(blockState, property, valueName, must);
  }

  @Override
  public @NotNull Type getType() {
    return Type.SIMPLE;
  }

}
