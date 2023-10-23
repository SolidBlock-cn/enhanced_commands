package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ParsingUtil;

public enum ParenthesesBlockPredicateType implements BlockPredicateType<BlockPredicate> {
  PARENTHESES_TYPE;

  @Override
  public @NotNull BlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
    return BlockPredicate.fromNbt(nbtCompound.getCompound("predicate"), world);
  }

  @Override
  public @Nullable BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return ParsingUtil.parseParentheses(() -> BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly, true), parser);
  }
}
