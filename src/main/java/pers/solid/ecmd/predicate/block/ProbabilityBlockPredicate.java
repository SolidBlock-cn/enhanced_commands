package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.Set;

/**
 * <p>The predicate that passes only a probability test is passed.</p>
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code probability(<probability>)} - passes under a specified probability.</li>
 *   <li>{@code probability(<probability>, <predicate>)} - passes when both probability test and another block predicate passes. Identical to {@code all(probability(<probability>), <predicate>)}.</li>
 *   </ul>
 */
public record ProbabilityBlockPredicate(float probability, @NotNull BlockPredicate predicate, OptionalLong seed) implements BlockPredicate {
  public static final MapCodec<ProbabilityBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(ProbabilityBlockPredicate::new,
      Codec.FLOAT.fieldOf("probability").forGetter(ProbabilityBlockPredicate::probability),
      BlockPredicate.CODEC.optionalFieldOf("predicate", ConstantBlockPredicate.ALWAYS_TRUE).forGetter(ProbabilityBlockPredicate::predicate),
      CodecUtil.optionalLongFieldOf("seed").forGetter(ProbabilityBlockPredicate::seed)));

  @Override
  public @NotNull String asString() {
    final String seedParams = seed.isPresent() ? ", seed = " + seed.getAsLong() : "";
    if (predicate == ConstantBlockPredicate.ALWAYS_TRUE) {
      return "probability(" + probability + seedParams + ")";
    } else {
      return "probability(" + probability + ", " + predicate.asString() + seedParams + ")";
    }
  }

  @Override
  public boolean test(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    final RandomSource random = context.getSplitterForOptionalSeed(this, seed).at(cachedBlockPosition.getPos());
    if (predicate == ConstantBlockPredicate.ALWAYS_TRUE) {
      return random.nextFloat() < probability;
    } else {
      return random.nextFloat() < probability && predicate.test(cachedBlockPosition, context);
    }
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    final float nextFloat = context.getSplitterForOptionalSeed(this, seed).at(cachedBlockPosition.getPos()).nextFloat();
    final MutableComponent o1 = Component.literal(String.valueOf(nextFloat)).withStyle(Styles.ACTUAL);
    final MutableComponent o2 = Component.literal(String.valueOf(probability)).withStyle(Styles.EXPECTED);
    if (nextFloat < probability) {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.probability.pass", o1, o2));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.probability.fail", o1, o2));
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.RAND;
  }

  public enum Type implements BlockPredicateType<ProbabilityBlockPredicate> {
    RAND_TYPE;

    @Override
    public @NotNull MapCodec<ProbabilityBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.MixedParams<ProbabilityBlockPredicate> {
    private float value;
    private BlockPredicate predicate;
    private OptionalLong seed = OptionalLong.empty();

    @Override
    public ProbabilityBlockPredicate getParseResult(ParseContext<?> parseContext) {
      if (predicate == null) {
        predicate = ConstantBlockPredicate.ALWAYS_TRUE;
      }
      return new ProbabilityBlockPredicate(value, predicate, seed);
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }

    private static final Set<String> SUPPORTED_PARAMS = Set.of("seed");

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        value = reader.readFloat();
        if (value > 1) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(reader, value, 1);
        }
        if (value < 0) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(reader, value, 0);
        }
      } else if (paramIndex == 1) {
        predicate = BlockPredicate.parse(parseContext);
      }
    }

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return seed.isPresent();
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      seed = OptionalLong.of(parseContext.reader().readLong());
    }
  }
}
