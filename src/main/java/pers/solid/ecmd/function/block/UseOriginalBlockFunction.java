package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

public enum UseOriginalBlockFunction implements BlockFunction {
  USE_ORIGINAL;

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
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
    public @Nullable UseOriginalBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestions.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("~", Text.translatable("enhancedCommands.argument.block_function.use_original"));
        }
      });
      if (parser.reader.canRead() && parser.reader.peek() == '~') {
        parser.reader.skip();
        parser.suggestions.clear();
        return USE_ORIGINAL;
      }
      return null;
    }
  }
}
