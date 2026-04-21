package pers.solid.ecmd.property.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import pers.solid.ecmd.util.StateUtil;

import java.util.Collections;
import java.util.Set;

public record AllRandomPropertyNameFunction(Set<String> except) implements GeneralPropertyFunction.OfName {
  public static final MapCodec<AllRandomPropertyNameFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(AllRandomPropertyNameFunction::new, Codec.STRING.listOf().<Set<String>>xmap(ImmutableSet::copyOf, ImmutableList::copyOf).fieldOf("except").forGetter(AllRandomPropertyNameFunction::except)));

  public AllRandomPropertyNameFunction() {
    this(Collections.emptySet());
  }

  @Override
  public String expressAsString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, RandomSource random) {
    if (except.isEmpty()) {
      return StateUtil.getBlockWithRandomProperties(blockState.getBlock(), random);
    } else {
      for (Property<?> property : blockState.getProperties()) {
        if (!except.contains(property.getName())) {
          blockState = StateUtil.withPropertyOfRandomValue(blockState, property, random);
        }
      }
      return blockState;
    }
  }

  @Override
  public String propertyName() {
    return null;
  }

  @Override
  public Type getType() {
    return Type.ALL_RANDOM;
  }

}
