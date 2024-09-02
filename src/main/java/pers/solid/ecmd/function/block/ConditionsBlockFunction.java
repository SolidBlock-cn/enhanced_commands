package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public record ConditionsBlockFunction(@NotNull List<ConditionalBlockFunction> conditions) implements BlockFunction {
  public static final MapCodec<ConditionsBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codecs.nonEmptyList(ConditionalBlockFunction.CODEC.codec().listOf()).fieldOf("conditions").forGetter(ConditionsBlockFunction::conditions)).apply(i, ConditionsBlockFunction::new));

  public ConditionsBlockFunction(@NotNull ConditionalBlockFunction... conditions) {
    this(List.of(conditions));
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, pos, false);
    for (ConditionalBlockFunction function : conditions) {
      if (function.condition().test(cachedBlockPosition)) {
        return function.functionIfTrue().getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
      }
    }
    if (!conditions.isEmpty()) {
      return conditions.getLast().getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    } else {
      return origState;
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.CONDITIONS;
  }

  @Override
  public @NotNull String asString() {
    return conditions.stream().map(f -> f.condition().asString() + ", " + f.functionIfTrue().asString() + (f.functionIfFalse() == EmptyBlockFunction.INSTANCE ? "" : ", " + f.functionIfFalse().asString())).collect(Collectors.joining("; ", "ifs(", ")"));
  }

  public enum Type implements BlockFunctionType<ConditionsBlockFunction> {
    CONDITIONS_TYPE;

    @Override
    public @NotNull MapCodec<ConditionsBlockFunction> getCodec() {
      return CODEC;
    }
  }
}
