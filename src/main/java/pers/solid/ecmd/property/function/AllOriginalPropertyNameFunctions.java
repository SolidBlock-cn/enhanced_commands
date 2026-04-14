package pers.solid.ecmd.property.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.Set;

public record AllOriginalPropertyNameFunctions(@NotNull Set<String> except) implements GeneralPropertyFunction.OfName {
  public static final MapCodec<AllOriginalPropertyNameFunctions> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(AllOriginalPropertyNameFunctions::new, CodecUtil.optionalField("except", Codec.STRING.listOf().<Set<String>>xmap(ImmutableSet::copyOf, ImmutableList::copyOf), Collections.emptySet()).forGetter(AllOriginalPropertyNameFunctions::except)));

  public AllOriginalPropertyNameFunctions() {
    this(Collections.emptySet());
  }

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, RandomSource random) {
    for (Property<?> property : blockState.getProperties()) {
      if (!except.contains(property.getName()) && origState.hasProperty(property)) {
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
