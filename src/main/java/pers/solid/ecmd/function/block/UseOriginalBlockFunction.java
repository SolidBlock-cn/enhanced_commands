package pers.solid.ecmd.function.block;

import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.SuggestionUtil;

public enum UseOriginalBlockFunction implements BlockFunction {
  USE_ORIGINAL;

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    return origState;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {}

  @Override
  public @NotNull BlockFunctionType<UseOriginalBlockFunction> getType() {
    return BlockFunctionTypes.USE_ORIGINAL;
  }

  public enum Type implements BlockFunctionType<UseOriginalBlockFunction> {
    USE_ORIGINAL_TYPE;

    @Override
    public UseOriginalBlockFunction fromNbt(NbtCompound nbtCompound) {
      return USE_ORIGINAL;
    }

    @Override
    public @Nullable UseOriginalBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("~", Text.translatable("enhancedCommands.argument.block_function.use_original"), suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '~') {
        parser.reader.skip();
        parser.suggestionProviders.clear();
        return USE_ORIGINAL;
      }
      return null;
    }
  }
}
