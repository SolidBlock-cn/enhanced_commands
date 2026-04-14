package pers.solid.ecmd.property.function;

import com.google.common.collect.Collections2;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.Set;

public record AllRandomPropertyFunction(@NotNull Set<Property<?>> except) implements GeneralPropertyFunction {
  public AllRandomPropertyFunction() {
    this(Collections.emptySet());
  }

  public static MapCodec<AllRandomPropertyFunction> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.ap(AllRandomPropertyFunction::new, CodecUtil.set(CodecUtil.propertyForBlock(block.getStateDefinition())).optionalFieldOf("except", Collections.emptySet()).forGetter(AllRandomPropertyFunction::except)));
  }

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, RandomSource random) {
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
