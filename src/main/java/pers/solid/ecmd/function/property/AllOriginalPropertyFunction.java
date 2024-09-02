package pers.solid.ecmd.function.property;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.Set;

public record AllOriginalPropertyFunction(@NotNull Set<Property<?>> except) implements GeneralPropertyFunction {
  public AllOriginalPropertyFunction() {
    this(Collections.emptySet());
  }

  public static MapCodec<AllOriginalPropertyFunction> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.ap(AllOriginalPropertyFunction::new, CodecUtil.set(CodecUtil.propertyForBlock(block.getStateManager())).optionalFieldOf("except", Collections.emptySet()).forGetter(AllOriginalPropertyFunction::except)));
  }

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, Random random) {
    for (Property<?> property : blockState.getProperties()) {
      if (!except.contains(property) && origState.contains(property)) {
        blockState = StateUtil.withPropertyOfValueFromAnother(blockState, origState, property);
      }
    }
    return blockState;
  }

  @Override
  public Property<Integer> property() {
    return null;
  }

  @Override
  public Type getType() {
    return Type.ALL_ORIGINAL;
  }

}
