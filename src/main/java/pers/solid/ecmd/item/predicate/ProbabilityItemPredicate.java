package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.Set;

public record ProbabilityItemPredicate(float probability, @NotNull ItemPredicate predicate, OptionalLong seed) implements ItemPredicateEntry {
  public static final MapCodec<ProbabilityItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(ProbabilityItemPredicate::new,
      Codec.FLOAT.fieldOf("probability").forGetter(ProbabilityItemPredicate::probability),
      ItemPredicate.CODEC.optionalFieldOf("predicate", ConstantItemPredicate.ALWAYS_TRUE).forGetter(ProbabilityItemPredicate::predicate),
      CodecUtil.optionalLongFieldOf("seed").forGetter(ProbabilityItemPredicate::seed)));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    final RandomSource random = executionContext.getSplitterForOptionalSeed(this, seed).at(BlockPos.containing(executionContext.positionProvider.getPosition$ec()));
    if (predicate == ConstantItemPredicate.ALWAYS_TRUE) {
      return random.nextFloat() < probability;
    } else {
      return random.nextFloat() < probability && predicate.test(stack, executionContext);
    }
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.PROBABILITY;
  }

  @Override
  public @NotNull String asString() {
    final String seedParams = seed.isPresent() ? ", seed = " + seed.getAsLong() : "";
    if (predicate == ConstantItemPredicate.ALWAYS_TRUE) {
      return "probability(" + probability + seedParams + ")";
    } else {
      return "probability(" + probability + ", " + predicate.asString() + seedParams + ")";
    }
  }

  public enum Type implements ItemPredicateType<ProbabilityItemPredicate> {
    PROBABILITY_TYPE;

    @Override
    public @NotNull MapCodec<ProbabilityItemPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionContentParser.MixedParams<ProbabilityItemPredicate> {
    private float value;
    private ItemPredicate predicate;
    private OptionalLong seed = OptionalLong.empty();

    @Override
    public ProbabilityItemPredicate getParseResult(ParseContext<?> parseContext) {
      if (predicate == null) {
        predicate = ConstantItemPredicate.ALWAYS_TRUE;
      }
      return new ProbabilityItemPredicate(value, predicate, seed);
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
        value = FloatArgumentType.floatArg(0, 1).parse(reader);
      } else if (paramIndex == 1) {
        predicate = ItemPredicate.parse(parseContext);
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
