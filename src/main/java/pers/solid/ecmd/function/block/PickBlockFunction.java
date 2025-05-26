package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.Collection;
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
  public static final SimpleCommandExceptionType SUM_ZERO = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.block_function.pick.zero_sum"));
  public static final MapCodec<PickBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(WeightedList.createMapCodec(BlockFunction.CODEC).forGetter(PickBlockFunction::functions), CodecUtil.optionalLongFieldOf("seed").forGetter(PickBlockFunction::seed)).apply(instance, PickBlockFunction::new));

  public PickBlockFunction(WeightedList<BlockFunction> functions) {
    this(functions, OptionalLong.empty());
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.PICK;
  }

  public enum Type implements BlockFunctionType<PickBlockFunction> {
    PICK_TYPE;

    @Override
    public @NotNull MapCodec<PickBlockFunction> getCodec() {
      return CODEC;
    }
  }


  @Override
  public @NotNull String asString() {
    return functions.asStringStream(ExpressionConvertible::asString).collect(Collectors.joining(", ", "pick(", (seed.isPresent() ? "; seed = " + seed.getAsLong() : "") + ")"));
  }


  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Random random = context.getSplitterForOptionalSeed(this, seed).split(pos);
    return functions.getRandom(random).getModifiedState(blockState, origState, world, pos, blockEntityData, context);
  }


  public static class Parser implements FunctionLikeParser<BlockFunctionArgument>, NamedParamListParser {
    private WeightedList<BlockFunctionArgument> weightedList;
    private OptionalLong seed = OptionalLong.empty();
    private static final Set<String> SUPPORTED_PARAMS = Set.of("seed");

    @Override
    public BlockFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return source -> new PickBlockFunction(weightedList.transform(blockFunctionArgument -> blockFunctionArgument.apply(source)), seed);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final WeightedListParser<BlockFunctionArgument> weightedListParser = WeightedListParser.of((parseContext1) -> BlockFunctionArgument.parse(parseContext));
      final StringReader reader = parseContext.parser().reader;

      weightedList = weightedListParser.parse(parseContext);

      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.parser().clearSuggestion();

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
      seed = OptionalLong.of(parseContext.parser().reader.readLong());
    }
  }
}
