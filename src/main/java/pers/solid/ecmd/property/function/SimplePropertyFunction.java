package pers.solid.ecmd.property.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 简单地设置将方块的某个属性设置为值。其中，{@code must} 参考用于控制方块状态不存在时，是直接失败还是不执行。但由于命令解析时就已经判断好了方块状态是否存在，因此此参数的作用不大。
 *
 * @param must 指定方块状态不存在时的行为。
 */
public record SimplePropertyFunction<T extends Comparable<T>>(Property<T> property, T value, boolean must) implements PropertyFunction<T> {
  public static MapCodec<SimplePropertyFunction<?>> getCodec(Block block) {
    return CodecUtil.propertyForBlock(block.getStateDefinition()).dispatchMap("property", SimplePropertyFunction::property, SimplePropertyFunction::createCodecForProperty);
  }

  private static <T extends Comparable<T>> MapCodec<SimplePropertyFunction<T>> createCodecForProperty(Property<T> property) {
    return RecordCodecBuilder.mapCodec(i -> i.apply2((value, must) -> new SimplePropertyFunction<>(property, value, must),
        property.codec().fieldOf("value").forGetter(SimplePropertyFunction::value),
        Codec.BOOL.optionalFieldOf("must", false).forGetter(SimplePropertyFunction::must)));
  }

  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==" : "=") + property.getName(value);
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, RandomSource random) {
    if (must || blockState.hasProperty(property)) {
      return blockState.setValue(property, value);
    } else {
      return blockState;
    }
  }

  @Override
  public Type getType() {
    return Type.SIMPLE;
  }
}
