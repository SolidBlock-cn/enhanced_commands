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

public record AllRandomPropertyNameFunction(@NotNull Set<String> except) implements GeneralPropertyFunction.OfName {
  public static final Codec<AllRandomPropertyNameFunction> CODEC = RecordCodecBuilder.create(i -> i.ap(AllRandomPropertyNameFunction::new, Codec.STRING.listOf().<Set<String>>xmap(ImmutableSet::copyOf, ImmutableList::copyOf).fieldOf("except").forGetter(AllRandomPropertyNameFunction::except)));

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState origState, BlockState blockState, Random random) {
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
  public @NotNull Type getType() {
    return Type.ALL_RANDOM;
  }

}
