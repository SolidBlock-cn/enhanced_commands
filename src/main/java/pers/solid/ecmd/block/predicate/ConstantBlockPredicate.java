package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.Collections;

public enum ConstantBlockPredicate implements BlockPredicate {
  ALWAYS_TRUE(true),
  ALWAYS_FALSE(false);
  private final boolean value;

  public static final MapCodec<ConstantBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.BOOL.optionalFieldOf("value", true).forGetter(ConstantBlockPredicate::value)).apply(i, ConstantBlockPredicate::of));

  ConstantBlockPredicate(boolean value) {
    this.value = value;
  }

  public boolean value() {
    return value;
  }

  public static ConstantBlockPredicate of(boolean value) {
    return value ? ALWAYS_TRUE : ALWAYS_FALSE;
  }

  @Override
  public String expressAsString() {
    return value ? "*" : "!*";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return value;
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    if (value) {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.constant.pass"));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.constant.fail"));
    }
  }

  @Override
  public BlockPredicateType<ConstantBlockPredicate> getType() {
    return BlockPredicateTypes.CONSTANT;
  }

  @Override
  public Iterable<? extends RequiresValidation> membersToValidate() {
    return Collections.emptyList();
  }

  public enum ConstantParser implements Parser<ConstantBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable ConstantBlockPredicate parse(ParseContext<?> parseContext) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("*", Component.translatable("enhanced_commands.block_predicate.constant"), suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '*') {
        reader.skip();
        parseContext.clearSuggestion();
        return ALWAYS_TRUE;
      } else {
        return null;
      }
    }
  }
}
