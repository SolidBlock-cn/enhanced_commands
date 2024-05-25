package pers.solid.ecmd.function.property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;

import java.util.Set;

public record AllOriginalPropertyNameFunctions(@NotNull Set<String> except) implements GeneralPropertyFunction.OfName {
  public static final Codec<AllOriginalPropertyNameFunctions> CODEC = RecordCodecBuilder.create(i -> i.ap(AllOriginalPropertyNameFunctions::new, Codec.STRING.listOf().<Set<String>>xmap(ImmutableSet::copyOf, ImmutableList::copyOf).fieldOf("except").forGetter(AllOriginalPropertyNameFunctions::except)));

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
    for (Property<?> property : blockState.getProperties()) {
      if (!except.contains(property.getName()) && origState.contains(property)) {
        blockState = StateUtil.withPropertyOfValueFromAnother(blockState, origState, property);
      }
    }
    return blockState;
  }

  @Override
  public String propertyName() {
    return null;
  }

  @Override
  public @NotNull Type getType() {
    return Type.ALL_ORIGINAL;
  }

}
