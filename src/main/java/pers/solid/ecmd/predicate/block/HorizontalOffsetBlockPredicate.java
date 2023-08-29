package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import joptsimple.internal.Strings;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.SuggestionUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record HorizontalOffsetBlockPredicate(int offset, BlockPredicate blockPredicate) implements BlockPredicate {
  public static final Text ABOVE_BLOCK = Text.translatable("enhancedCommands.argument.block_predicate.above_block");
  public static final Text BENEATH_BLOCK = Text.translatable("enhancedCommands.argument.block_predicate.beneath_block");

  @Override
  public @NotNull String asString() {
    if (offset > 0) {
      return Strings.repeat('>', offset) + blockPredicate.asString();
    } else if (offset < 0) {
      return Strings.repeat('<', -offset) + blockPredicate.asString();
    } else {
      return blockPredicate.asString();
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return blockPredicate.test(new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().up(offset), false));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final Text description;
    final CachedBlockPosition offsetPosition = new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().up(offset), false);
    final TestResult attachment = blockPredicate.testAndDescribe(offsetPosition);
    final boolean successes = attachment.successes();
    final String string = successes ? "pass" : "fail";
    final Formatting formatting = successes ? Formatting.GREEN : Formatting.RED;
    if (offset > 0) {
      description = Text.translatable("enhancedCommands.argument.block_predicate.test_relative_above_" + string, offset, TextUtil.wrapBlockPos(cachedBlockPosition.getBlockPos())).formatted(formatting);
    } else {
      description = Text.translatable("enhancedCommands.argument.block_predicate.test_relative_below_" + string, -offset, TextUtil.wrapBlockPos(cachedBlockPosition.getBlockPos())).formatted(formatting);
    }
    return new TestResult(successes, List.of(description), List.of(attachment));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.HORIZONTAL_OFFSET;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putInt("offset", offset);
    nbtCompound.put("predicate", blockPredicate.createNbt());
  }

  public enum Type implements BlockPredicateType<HorizontalOffsetBlockPredicate> {
    HORIZONTAL_OFFSET_TYPE;

    @Override
    public @NotNull HorizontalOffsetBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      final int offset = nbtCompound.getInt("offset");
      final BlockPredicate predicate = BlockPredicate.fromNbt(nbtCompound.getCompound("predicate"));
      return new HorizontalOffsetBlockPredicate(offset, predicate);
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestions.add((context, suggestionsBuilder) -> {
        SuggestionUtil.suggestString("<", BENEATH_BLOCK, suggestionsBuilder);
        SuggestionUtil.suggestString(">", ABOVE_BLOCK, suggestionsBuilder);
      });
      int offset = 0;
      boolean prefixed = false;
      final StringReader reader = parser.reader;
      if (!reader.canRead())
        return null;
      while (reader.canRead()) {
        if (reader.peek() == '>') {
          offset -= 1;
          prefixed = true;
          reader.skip();
        } else if (reader.peek() == '<') {
          offset += 1;
          prefixed = true;
          reader.skip();
        } else {
          break;
        }
      }
      if (offset != 0) {
        return new HorizontalOffsetBlockPredicate(offset, BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly));
      } else if (prefixed) {
        return BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
      return null;
    }
  }
}
