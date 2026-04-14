package pers.solid.ecmd.property.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.Set;

public record AllOriginalPropertyFunction(Set<Property<?>> except) implements GeneralPropertyFunction {
  public AllOriginalPropertyFunction() {
    this(Collections.emptySet());
  }

  public static MapCodec<AllOriginalPropertyFunction> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.ap(AllOriginalPropertyFunction::new, CodecUtil.set(CodecUtil.propertyForBlock(block.getStateDefinition())).optionalFieldOf("except", Collections.emptySet()).forGetter(AllOriginalPropertyFunction::except)));
  }

  @Override
  public String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, RandomSource random) {
    for (Property<?> property : blockState.getProperties()) {
      if (!except.contains(property) && origState.hasProperty(property)) {
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
