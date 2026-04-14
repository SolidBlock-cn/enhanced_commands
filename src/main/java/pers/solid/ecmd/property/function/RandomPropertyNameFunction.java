package pers.solid.ecmd.property.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record RandomPropertyNameFunction(String propertyName, boolean must) implements PropertyNameFunction {
  public static final MapCodec<RandomPropertyNameFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(RandomPropertyNameFunction::new, Codec.STRING.fieldOf("property").forGetter(RandomPropertyNameFunction::propertyName), Codec.BOOL.optionalFieldOf("must", false).forGetter(RandomPropertyNameFunction::must)));

  @Override
  public String asString() {
    return propertyName + (must ? "==*" : "=*");
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, RandomSource random) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return getModifiedStateForProperty(blockState, property);
  }

  @Override
  public Type getType() {
    return Type.RANDOM;
  }

  private <T extends Comparable<T>> BlockState getModifiedStateForProperty(BlockState blockState, Property<T> property) {
    final List<T> values = List.copyOf(property.getPossibleValues());
    return blockState.setValue(property, values.get(ThreadLocalRandom.current().nextInt(values.size())));
  }

}
