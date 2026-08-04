package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.NamedParamListParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从多个方块函数中随机选择一个来生成。可以带有权重。例如：
 * <pre>
 *   pick(white_wool, black_wool)  随机从白色羊毛和黑色羊毛中选择，概率相等
 *   pick(white_wool 1, black_wool 2)   黑色羊毛被选择的概率是白色羊毛的两倍
 * </pre>
 * 当概率数值之和不为 1 时，会除以其总和，以使得各个部分的概率为 1。
 * <p>
 * 允许零值，但总和不能为零。
 */
public record PickBlockFunction(WeightedList<BlockFunction> functions, OptionalLong seed) implements BlockFunction {
  public static final MapCodec<PickBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(WeightedList.createMapCodec(BlockFunction.CODEC).forGetter(PickBlockFunction::functions), CodecUtil.optionalLongFieldOf("seed").forGetter(PickBlockFunction::seed)).apply(instance, PickBlockFunction::new));

  public PickBlockFunction(WeightedList<BlockFunction> functions) {
    this(functions, OptionalLong.empty());
  }

  @Override
  public BlockFunctionType<PickBlockFunction> getType() {
    return BlockFunctionTypes.PICK;
  }

  @Override
  public String expressAsString() {
    return functions.asStringStream(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ", "pick(", (seed.isPresent() ? "; seed = " + seed.getAsLong() : "") + ")"));
  }


  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    final RandomSource random = context.getSplitterForOptionalSeed(this, seed).at(pos);
    return functions.getRandom(random).getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return functions;
  }


  public static class Parser implements FunctionContentParser<BlockFunction>, NamedParamListParser {
    private @Nullable WeightedList<BlockFunction> weightedList;
    private OptionalLong seed = OptionalLong.empty();
    private static final Set<String> SUPPORTED_PARAMS = Set.of("seed");

    @Override
    public BlockFunction getParseResult(ParseContext<?> parseContext) {
      return new PickBlockFunction(Objects.requireNonNull(weightedList, "weightedList"), seed);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final WeightedListParser<BlockFunction> weightedListParser = WeightedListParser.of((parseContext1) -> BlockFunction.parse(parseContext));
      final StringReader reader = parseContext.reader();

      weightedList = weightedListParser.parse(parseContext);

      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        parseNamedParameters(parseContext);
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
