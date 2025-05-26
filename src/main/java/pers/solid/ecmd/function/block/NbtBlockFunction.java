package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtFunctionSuggestedParser;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record NbtBlockFunction(@NotNull CompoundNbtFunction nbtFunction) implements BlockFunction {
  public static final MapCodec<NbtBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NbtBlockFunction::new, CompoundNbtFunction.CODEC.fieldOf("nbt").forGetter(NbtBlockFunction::nbtFunction)));

  @Override
  public @NotNull String asString() {
    return nbtFunction.asString(false);
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    try {
      blockEntityData.setValue(nbtFunction.apply(blockEntityData.getValue()));
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
    return blockState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.NBT;
  }

  public enum Type implements BlockFunctionType<NbtBlockFunction>, Parser<BlockFunctionArgument> {
    NBT_TYPE;

    @Override
    public @NotNull MapCodec<NbtBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable NbtBlockFunction parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder).buildFuture());
      if (parser.reader.canRead() && parser.reader.peek() == '{') {
        return new NbtBlockFunction(new NbtFunctionSuggestedParser<>(parser).parseCompound(false));
      } else {
        return null;
      }
    }
  }
}
