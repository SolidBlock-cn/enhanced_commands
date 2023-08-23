package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.SuggestionUtil;

public record NbtBlockPredicate(NbtPredicate nbtPredicate) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return nbtPredicate.asString(false);
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
    return blockEntity != null && nbtPredicate.test(blockEntity.createNbt());
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NBT;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("nbtPredicate", nbtPredicate.asString());
  }

  public enum Type implements BlockPredicateType<NbtBlockPredicate> {
    NBT_TYPE;

    @Override
    public @NotNull NbtBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      final String s = nbtCompound.getString("nbtPredicate");
      try {
        return new NbtBlockPredicate(new NbtPredicateSuggestedParser(new StringReader(s)).parseCompound(false, false));
      } catch (CommandSyntaxException e) {
        throw new IllegalArgumentException("Cannot parse nbt: " + s, e);
      }
    }

    @Override
    public @Nullable NbtBlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '{') {
        return new NbtBlockPredicate(new NbtPredicateSuggestedParser(parser.reader, parser.suggestions).parseCompound(false, false));
      } else {
        return null;
      }
    }
  }
}
