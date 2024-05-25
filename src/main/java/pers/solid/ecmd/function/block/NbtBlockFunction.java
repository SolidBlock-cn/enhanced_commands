package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
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
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;

public record NbtBlockFunction(@NotNull CompoundNbtFunction nbtFunction) implements BlockFunction {
  public static final Codec<NbtBlockFunction> CODEC = RecordCodecBuilder.create(i -> i.ap(NbtBlockFunction::new, CompoundNbtFunction.CODEC.fieldOf("nbt").forGetter(NbtBlockFunction::nbtFunction)));

  @Override
  public @NotNull String asString() {
    return nbtFunction.asString(false);
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    blockEntityData.setValue(nbtFunction.apply(blockEntityData.getValue()));
    return blockState;
  }

  @Override
  public @NotNull BlockFunctionType<NbtBlockFunction> getType() {
    return BlockFunctionTypes.NBT;
  }

  public enum Type implements BlockFunctionType<NbtBlockFunction>, Parser<BlockFunctionArgument> {
    NBT_TYPE;

    @Override
    public @NotNull Codec<NbtBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable NbtBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '{') {
        return new NbtBlockFunction(new NbtFunctionSuggestedParser(parser.reader, parser.suggestionProviders).parseCompound(false));
      } else {
        return null;
      }
    }
  }
}
