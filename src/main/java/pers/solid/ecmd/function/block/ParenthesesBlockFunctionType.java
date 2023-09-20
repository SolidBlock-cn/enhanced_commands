package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.SuggestionUtil;

public enum ParenthesesBlockFunctionType implements BlockFunctionType<BlockFunction> {
  PARENTHESES_TYPE;

  @Override
  public BlockFunction fromNbt(NbtCompound nbtCompound) {
    return BlockFunction.fromNbt(nbtCompound.getCompound("function"));
  }

  @Override
  public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    return SuggestionUtil.parseParentheses(() -> BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly, true), parser);
  }
}
