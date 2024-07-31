package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.List;

/**
 * 给予一个随机的方块状态属性。
 */
public record RandomPropertyFunction<T extends Comparable<T>>(Property<T> property, boolean must) implements PropertyFunction<T> {
  private static final RecordCodecBuilder<RandomPropertyFunction<?>, Boolean> MUST_FIELD_CODEC = Codec.BOOL.optionalFieldOf("must", false).forGetter(RandomPropertyFunction::must);

  public static MapCodec<RandomPropertyFunction<?>> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.apply2(
        RandomPropertyFunction::new,
        CodecUtil.propertyForBlock(block.getStateManager()).fieldOf("property").forGetter(RandomPropertyFunction::property),
        MUST_FIELD_CODEC
    ));
  }

  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==*" : "=*");
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, Random random) {
    if (must || blockState.contains(property)) {
      final List<T> values = List.copyOf(property.getValues());
      return blockState.with(property, values.get(random.nextInt(values.size())));
    } else {
      return blockState;
    }
  }

  @Override
  public Type getType() {
    return Type.RANDOM;
  }

}
