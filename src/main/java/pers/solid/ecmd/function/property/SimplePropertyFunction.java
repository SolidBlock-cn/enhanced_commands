package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 简单地设置将方块的某个属性设置为值。其中，{@code must} 参考用于控制方块状态不存在时，是直接失败还是不执行。但由于命令解析时就已经判断好了方块状态是否存在，因此此参数的作用不大。
 *
 * @param must 指定方块状态不存在时的行为。
 */
public record SimplePropertyFunction<T extends Comparable<T>>(Property<T> property, T value, boolean must) implements PropertyFunction<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==" : "=") + property.name(value);
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, Random random) {
    if (must || blockState.contains(property)) {
      return blockState.with(property, value);
    } else {
      return blockState;
    }
  }

  @Override
  public Type getType() {
    return Type.SIMPLE;
  }

  public static Codec<SimplePropertyFunction<?>> getCodec(Block block) {
    return CodecUtil.propertyForBlock(block.getStateManager()).dispatch("property", SimplePropertyFunction::property, SimplePropertyFunction::createCodecForProperty);
  }

  private static <T extends Comparable<T>> Codec<SimplePropertyFunction<T>> createCodecForProperty(Property<T> property) {
    return RecordCodecBuilder.create(i -> i.apply2((value, must) -> new SimplePropertyFunction<>(property, value, must), property.getCodec().fieldOf("value").forGetter(SimplePropertyFunction::value), Codec.BOOL.optionalFieldOf("must", false).forGetter(SimplePropertyFunction::must)));
  }
}
