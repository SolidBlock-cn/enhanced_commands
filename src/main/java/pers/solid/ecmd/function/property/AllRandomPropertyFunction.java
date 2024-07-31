package pers.solid.ecmd.function.property;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Set;

public record AllRandomPropertyFunction(@NotNull Set<Property<?>> except) implements GeneralPropertyFunction {
  public static MapCodec<AllRandomPropertyFunction> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.ap(AllRandomPropertyFunction::new, CodecUtil.set(CodecUtil.propertyForBlock(block.getStateManager())).optionalFieldOf("except", ImmutableSet.of()).forGetter(AllRandomPropertyFunction::except)));
  }

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, Random random) {
    if (except.isEmpty()) {
      return StateUtil.getBlockWithRandomProperties(blockState.getBlock(), random);
    } else {
      for (Property<?> property : Collections2.filter(blockState.getProperties(), predicate -> !except.contains(predicate))) {
        blockState = StateUtil.withPropertyOfRandomValue(blockState, property, random);
      }
      return blockState;
    }
  }

  @Override
  public Property<Integer> property() {
    return null;
  }

  @Override
  public Type getType() {
    return Type.ALL_RANDOM;
  }

}
