package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import joptsimple.internal.Strings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.List;

public record HorizontalOffsetBlockPredicate(int offset, BlockPredicate blockPredicate) implements BlockPredicate {
  public static final Component ABOVE_BLOCK = Component.translatable("enhanced_commands.block_predicate.above_block");
  public static final Component BENEATH_BLOCK = Component.translatable("enhanced_commands.block_predicate.beneath_block");
  public static final MapCodec<HorizontalOffsetBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(HorizontalOffsetBlockPredicate::new, Codec.INT.fieldOf("offset").forGetter(HorizontalOffsetBlockPredicate::offset), BlockPredicate.CODEC.fieldOf("block_predicate").forGetter(HorizontalOffsetBlockPredicate::blockPredicate)));

  @Override
  public String expressAsString() {
    final String s = blockPredicate instanceof HorizontalOffsetBlockPredicate ? "(" + blockPredicate.expressAsString() + ")" : blockPredicate.expressAsString();
    if (offset > 0) {
      return Strings.repeat('<', offset) + s;
    } else if (offset < 0) {
      return Strings.repeat('>', -offset) + s;
    } else {
      return s;
    }
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return blockPredicate.test(new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos().above(offset), false), executionContext);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final MutableComponent description;
    final BlockInWorld offsetPosition = new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos().above(offset), false);
    final TestResult attachment = blockPredicate.testAndDescribe(offsetPosition, executionContext);
    final boolean successes = attachment.successes();
    final String string = successes ? "pass" : "fail";
    if (offset > 0) {
      description = Component.translatable("enhanced_commands.block_predicate.horizontal_offset.above_" + string, offset, TextUtil.wrapVector(blockInWorld.getPos()));
    } else {
      description = Component.translatable("enhanced_commands.block_predicate.horizontal_offset.below_" + string, -offset, TextUtil.wrapVector(blockInWorld.getPos()));
    }
    return TestResult.of(successes, description, List.of(attachment));
  }

  @Override
  public BlockPredicateType<HorizontalOffsetBlockPredicate> getType() {
    return BlockPredicateTypes.HORIZONTAL_OFFSET;
  }

  @Override
  public Iterable<? extends RequiresValidation> membersToValidate() {
    return List.of(blockPredicate);
  }

  public enum HorizontalOffsetParser implements Parser<BlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        ParsingUtil.suggestString("<", BENEATH_BLOCK, suggestionsBuilder);
        ParsingUtil.suggestString(">", ABOVE_BLOCK, suggestionsBuilder);
        return suggestionsBuilder.buildFuture();
      });
      int offset = 0;
      boolean prefixed = false;
      final StringReader reader = parseContext.reader();
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
      if (parseContext.allowSparse()) reader.skipWhitespace();
      final BlockPredicate parse = BlockPredicate.parse(parseContext);
      if (offset != 0) {
        return new HorizontalOffsetBlockPredicate(offset, parse);
      } else {
        return parse;
      }
    }
  }
}
