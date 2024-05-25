package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

public record BypassingPropertyNameFunction(String propertyName, boolean must) implements PropertyNameFunction {
  public static final Codec<BypassingPropertyNameFunction> CODEC = RecordCodecBuilder.create(i -> i.apply2(BypassingPropertyNameFunction::new, Codec.STRING.fieldOf("property").forGetter(BypassingPropertyNameFunction::propertyName), Codec.BOOL.optionalFieldOf("must", false).forGetter(BypassingPropertyNameFunction::must)));

  @Override
  public @NotNull String asString() {
    return propertyName + (must ? "==~" : "=~");
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
    final Property<?> property = PropertyNameFunction.getProperty(blockState, propertyName, must);
    if (property == null) {
      return blockState;
    }
    return StateUtil.withPropertyOfValueFromAnother(blockState, origState, property);
  }

  @Override
  public @NotNull Type getType() {
    return Type.BYPASSING;
  }

}
