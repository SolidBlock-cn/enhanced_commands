package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ParsingUtil;

public enum ParenthesesBlockFunctionType implements BlockFunctionType<BlockFunction> {
  PARENTHESES_TYPE;

  @Override
  public @NotNull BlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
    return BlockFunction.fromNbt(nbtCompound.getCompound("function"), world);
  }

  @Override
  public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return ParsingUtil.parseParentheses(() -> BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly, true), parser);
  }
}
