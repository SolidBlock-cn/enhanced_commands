package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
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
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.Set;

/**
 * <p>The predicate that passes only a probability test is passed.</p>
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code rand(<probability>)} - passes under a specified probability.</li>
 *   <li>{@code rand(<probability>, <predicate>)} - passes when both probability test and another block predicate passes. Identical to {@code all(rand(probability), <predicate>)}.</li>
 *   </ul>
 */
public record RandBlockPredicate(float probability, @NotNull BlockPredicate predicate, OptionalLong seed) implements BlockPredicate {
  public static final MapCodec<RandBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(RandBlockPredicate::new,
      Codec.FLOAT.fieldOf("probability").forGetter(RandBlockPredicate::probability),
      BlockPredicate.CODEC.optionalFieldOf("predicate", ConstantBlockPredicate.ALWAYS_TRUE).forGetter(RandBlockPredicate::predicate),
      CodecUtil.optionalLongFieldOf("seed").forGetter(RandBlockPredicate::seed)));

  @Override
  public @NotNull String asString() {
    final String seedParams = seed.isPresent() ? "; seed = " + seed.getAsLong() : "";
    if (predicate == ConstantBlockPredicate.ALWAYS_TRUE) {
      return "rand(" + probability + seedParams + ")";
    } else {
      return "rand(" + probability + ", " + predicate.asString() + seedParams + ")";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final Random random = context.getSplitterForOptionalSeed(this, seed).split(cachedBlockPosition.getBlockPos());
    if (predicate == ConstantBlockPredicate.ALWAYS_TRUE) {
      return random.nextFloat() < probability;
    } else {
      return random.nextFloat() < probability && predicate.test(cachedBlockPosition, context);
    }
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final float nextFloat = context.getSplitterForOptionalSeed(this, seed).split(cachedBlockPosition.getBlockPos()).nextFloat();
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

  public static final class Parser implements FunctionParamsParser<BlockPredicateArgument>, NamedParamListParser {
    private float value;
    private BlockPredicateArgument predicate;
    private OptionalLong seed = OptionalLong.empty();

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) {
      if (predicate == null) {
        predicate = ConstantBlockPredicate.ALWAYS_TRUE;
      }
      return source -> new RandBlockPredicate(value, predicate.apply(source), seed);
    }

    private static final Set<String> SUPPORTED_PARAMS = Set.of("seed");

    @Override
    public int minParamsCount() {
      return 1;
    }

    public int maxParamsCount() {
      if (seed.isPresent() && predicate == null) {
        return 1;
      }
      return 2;
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      if (paramIndex == 0) {
        value = reader.readFloat();
        if (value > 1) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(reader, value, 1);
        }
        if (value < 0) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(reader, value, 0);
        }
      } else if (paramIndex == 1) {
        predicate = BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly);
      }

      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parser.clearSuggestion();

        parseNamedParameters(registryAccess, parser, suggestionsOnly);
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
    public void parseNamedParameter(String paramName, CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      seed = OptionalLong.of(parser.reader.readLong());
    }
  }
}
