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

/**
 * 此属性函数用于表示不改变方块状态属性，也就是说改变方块之后仍保留原来的方块状态属性的值。例如：
 * <pre>
 *   spruce_stairs[facing=~](oak_stairs[facing=east]) = oak_stairs[facing=west]
 * </pre>
 * {@code must} 参数用于控制当没有指定的方块状态属性时的行为。如果为 true，当不存在有关的方块状态属性时，抛出错误。如果为 false，则不执行。
 * <pre>
 *   spruce_stairs[facing=~](dirt) = spruce_stairs
 *   spruce_stairs[facing==~](dirt) = IllegalArgumentException
 * </pre>
 */
public record BypassingPropertyFunction<T extends Comparable<T>>(Property<T> property, boolean must) implements PropertyFunction<T> {
  private static final RecordCodecBuilder<BypassingPropertyFunction<?>, Boolean> MUST_FIELD_CODEC = Codec.BOOL.optionalFieldOf("must", false).forGetter(BypassingPropertyFunction::must);

  public static MapCodec<BypassingPropertyFunction<?>> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.apply2(
        BypassingPropertyFunction::new,
        CodecUtil.propertyForBlock(block.getStateManager()).fieldOf("property").forGetter(BypassingPropertyFunction::property),
        MUST_FIELD_CODEC
    ));
  }

  @Override
  public @NotNull String asString() {
    return property.getName() + (must ? "==~" : "=~");
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, Random random) {
    if (must || (blockState.contains(property) && origState.contains(property))) {
      return blockState.with(property, origState.get(property));
    } else {
      return blockState;
    }
  }

  @Override
  public Type getType() {
    return Type.BYPASSING;
  }

}
