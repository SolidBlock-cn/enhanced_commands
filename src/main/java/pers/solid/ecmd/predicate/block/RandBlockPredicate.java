package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;

import java.util.Random;

/**
 * <p>The predicate that passes only a probability test is passed.</p>
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code rand(<value>)} - passes under a specified probability.</li>
 *   <li>{@code rand(<value>, <predicate>)} - passes when both probability test and another block predicate passes. Identical to {@code all(rand(value), <predicate>)}.</li>
 *   </ul>
 */
public record RandBlockPredicate(float value, @Nullable BlockPredicate predicate) implements BlockPredicate {
  private static final Random RANDOM = new Random();

  @Override
  public @NotNull String asString() {
    if (predicate == null) {
      return "rand(" + value + ")";
    } else {
      return "rand(" + value + ", " + predicate.asString() + ")";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (predicate == null) {
      return RANDOM.nextFloat() < value;
    } else {
      return RANDOM.nextFloat() < value && predicate.test(cachedBlockPosition);
    }
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final float nextFloat = RandomUtils.nextFloat(0, 1);
    final MutableText o1 = Text.literal(String.valueOf(nextFloat)).styled(EnhancedCommands.STYLE_FOR_ACTUAL);
    final MutableText o2 = Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_EXPECTED);
    if (nextFloat < value) {
      return new TestResult(true, Text.translatable("blockPredicate.probability.pass", o1, o2).formatted(Formatting.GREEN));
    } else {
      return new TestResult(false, Text.translatable("blockPredicate.probability.fail", o1, o2).formatted(Formatting.RED));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.RAND;
  }

  public static final class Parser implements FunctionLikeParser<RandBlockPredicate> {
    private float value;
    private @Nullable BlockPredicate predicate;

    @Override
    public @NotNull String functionName() {
      return "rand";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("blockPredicate.probability");
    }

    @Override
    public RandBlockPredicate getParseResult() {
      return new RandBlockPredicate(value, predicate);
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    public int maxParamsCount() {
      return 2;
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        value = parser.reader.readFloat();
        if (value > 1) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(parser.reader, value, 1);
        }
        if (value < 0) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(parser.reader, value, 0);
        }
      } else if (paramIndex == 1) {
        predicate = BlockPredicate.parse(parser, suggestionsOnly);
      }
    }
  }

  public enum Type implements BlockPredicateType<RandBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(parser, suggestionsOnly);
    }
  }
}
