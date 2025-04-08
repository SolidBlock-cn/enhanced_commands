package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

/**
 * <p>The predicate that passes only a probability test is passed.</p>
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code rand(<probability>)} - passes under a specified probability.</li>
 *   <li>{@code rand(<probability>, <predicate>)} - passes when both probability test and another block predicate passes. Identical to {@code all(rand(probability), <predicate>)}.</li>
 *   </ul>
 */
public record RandBlockPredicate(float probability, @NotNull BlockPredicate predicate) implements BlockPredicate {
  public static final MapCodec<RandBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(RandBlockPredicate::new, Codec.FLOAT.fieldOf("probability").forGetter(RandBlockPredicate::probability), BlockPredicate.CODEC.optionalFieldOf("predicate", ConstantBlockPredicate.ALWAYS_TRUE).forGetter(RandBlockPredicate::predicate)));

  @Override
  public @NotNull String asString() {
    if (predicate == ConstantBlockPredicate.ALWAYS_TRUE) {
      return "rand(" + probability + ")";
    } else {
      return "rand(" + probability + ", " + predicate.asString() + ")";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final Random random = context.getSplitter(this).split(cachedBlockPosition.getBlockPos());
    if (predicate == ConstantBlockPredicate.ALWAYS_TRUE) {
      return random.nextFloat() < probability;
    } else {
      return random.nextFloat() < probability && predicate.test(cachedBlockPosition, context);
    }
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final float nextFloat = context.getSplitter(this).split(cachedBlockPosition.getBlockPos()).nextFloat();
    final MutableText o1 = Text.literal(String.valueOf(nextFloat)).styled(Styles.ACTUAL);
    final MutableText o2 = Text.literal(String.valueOf(probability)).styled(Styles.EXPECTED);
    if (nextFloat < probability) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.probability.pass", o1, o2));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.probability.fail", o1, o2));
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.RAND;
  }

  public enum Type implements BlockPredicateType<RandBlockPredicate> {
    RAND_TYPE;

    @Override
    public @NotNull MapCodec<RandBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    private float value;
    private @NotNull BlockPredicateArgument predicate = ConstantBlockPredicate.ALWAYS_TRUE;

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) {
      return source -> new RandBlockPredicate(value, predicate.apply(source));
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    public int maxParamsCount() {
      return 2;
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        value = parser.reader.readFloat();
        if (value > 1) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(parser.reader, value, 1);
        }
        if (value < 0) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(parser.reader, value, 0);
        }
      } else if (paramIndex == 1) {
        predicate = BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly);
      }
    }
  }
}
