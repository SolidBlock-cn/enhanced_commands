package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
        CodecUtil.propertyForBlock(block.getStateDefinition()).fieldOf("property").forGetter(RandomPropertyFunction::property),
        MUST_FIELD_CODEC
    ));
  }

  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==*" : "=*");
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, RandomSource random) {
    if (must || blockState.hasProperty(property)) {
      final List<T> values = List.copyOf(property.getPossibleValues());
      return blockState.setValue(property, values.get(random.nextInt(values.size())));
    } else {
      return blockState;
    }
  }

  @Override
  public Type getType() {
    return Type.RANDOM;
  }

}
