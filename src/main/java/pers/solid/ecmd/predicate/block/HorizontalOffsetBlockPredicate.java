package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import joptsimple.internal.Strings;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.List;

public record HorizontalOffsetBlockPredicate(int offset, BlockPredicate blockPredicate) implements BlockPredicate {
  public static final Text ABOVE_BLOCK = Text.translatable("enhanced_commands.block_predicate.above_block");
  public static final Text BENEATH_BLOCK = Text.translatable("enhanced_commands.block_predicate.beneath_block");
  public static final MapCodec<HorizontalOffsetBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(HorizontalOffsetBlockPredicate::new, Codec.INT.fieldOf("offset").forGetter(HorizontalOffsetBlockPredicate::offset), BlockPredicate.CODEC.fieldOf("block_predicate").forGetter(HorizontalOffsetBlockPredicate::blockPredicate)));

  @Override
  public @NotNull String asString() {
    final String s = blockPredicate instanceof HorizontalOffsetBlockPredicate ? "(" + blockPredicate.asString() + ")" : blockPredicate.asString();
    if (offset > 0) {
      return Strings.repeat('<', offset) + s;
    } else if (offset < 0) {
      return Strings.repeat('>', -offset) + s;
    } else {
      return s;
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return blockPredicate.test(new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().up(offset), false));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final MutableText description;
    final CachedBlockPosition offsetPosition = new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().up(offset), false);
    final TestResult attachment = blockPredicate.testAndDescribe(offsetPosition);
    final boolean successes = attachment.successes();
    final String string = successes ? "pass" : "fail";
    if (offset > 0) {
      description = Text.translatable("enhanced_commands.block_predicate.horizontal_offset.above_" + string, offset, TextUtil.wrapVector(cachedBlockPosition.getBlockPos()));
    } else {
      description = Text.translatable("enhanced_commands.block_predicate.horizontal_offset.below_" + string, -offset, TextUtil.wrapVector(cachedBlockPosition.getBlockPos()));
    }
    return TestResult.of(successes, description, List.of(attachment));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.HORIZONTAL_OFFSET;
  }

  public enum Type implements BlockPredicateType<HorizontalOffsetBlockPredicate>, Parser<BlockPredicateArgument> {
    HORIZONTAL_OFFSET_TYPE;

    @Override
    public @NotNull MapCodec<HorizontalOffsetBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockPredicateArgument parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.addSuggestion((context, suggestionsBuilder) -> {
        ParsingUtil.suggestString("<", BENEATH_BLOCK, suggestionsBuilder);
        ParsingUtil.suggestString(">", ABOVE_BLOCK, suggestionsBuilder);
        return suggestionsBuilder.buildFuture();
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
      if (!prefixed) return null;
      if (allowsSparse) reader.skipWhitespace();
      final BlockPredicateArgument parse = BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly, allowsSparse);
      if (offset != 0) {
        int finalOffset = offset;
        return source -> new HorizontalOffsetBlockPredicate(finalOffset, parse.apply(source));
      } else {
        return parse;
      }
    }
  }
}
