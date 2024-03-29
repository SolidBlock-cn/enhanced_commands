package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.TextUtil;

public record NbtBlockPredicate(@NotNull NbtPredicate nbtPredicate) implements BlockPredicate {
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
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
    final MutableText nameText = cachedBlockPosition.getBlockState().getBlock().getName();
    final MutableText posText = TextUtil.wrapVector(cachedBlockPosition.getBlockPos());
    if (blockEntity == null) {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.nbt.not_block_entity", nameText, posText));
    } else if (nbtPredicate.test(blockEntity.createNbt())) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.nbt.pass", nameText, posText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.nbt.fail"));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NBT;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("nbtPredicate", nbtPredicate.asString());
  }

  public enum Type implements BlockPredicateType<NbtBlockPredicate>, Parser<BlockPredicateArgument> {
    NBT_TYPE;

    @Override
    public @NotNull NbtBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final String s = nbtCompound.getString("nbtPredicate");
      try {
        return new NbtBlockPredicate(new NbtPredicateSuggestedParser(new StringReader(s)).parseCompound(false, false));
      } catch (CommandSyntaxException e) {
        throw new IllegalArgumentException("Cannot parse nbt: " + s, e);
      }
    }

    @Override
    public @Nullable NbtBlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '{') {
        return new NbtBlockPredicate(new NbtPredicateSuggestedParser(parser.reader, parser.suggestionProviders).parseCompound(false, false));
      } else {
        return null;
      }
    }
  }
}
