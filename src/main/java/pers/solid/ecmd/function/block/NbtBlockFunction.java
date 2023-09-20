package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import pers.solid.ecmd.util.SuggestionUtil;

public record NbtBlockFunction(@NotNull CompoundNbtFunction nbtFunction) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return nbtFunction.asString(false);
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    blockEntityData.setValue(nbtFunction.apply(blockEntityData.getValue()));
    return blockState;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("nbtFunction", nbtFunction.asString(false));
  }

  @Override
  public @NotNull BlockFunctionType<NbtBlockFunction> getType() {
    return BlockFunctionTypes.NBT;
  }


  public enum Type implements BlockFunctionType<NbtBlockFunction> {
    NBT_TYPE;

    @Override
    public @NotNull NbtBlockFunction fromNbt(@NotNull NbtCompound nbtCompound) {
      final String s = nbtCompound.getString("nbtPredicate");
      try {
        return new NbtBlockFunction(new NbtFunctionSuggestedParser(new StringReader(s)).parseCompound(false));
      } catch (CommandSyntaxException e) {
        throw new IllegalArgumentException("Cannot parse nbt: " + s, e);
      }
    }

    @Override
    public @Nullable NbtBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '{') {
        return new NbtBlockFunction(new NbtFunctionSuggestedParser(parser.reader, parser.suggestionProviders).parseCompound(false));
      } else {
        return null;
      }
    }
  }
}
