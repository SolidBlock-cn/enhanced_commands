package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.function.WeightedListParser;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.Objects;
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
public record PickNbtFunction(WeightedList<NbtFunction> functions) implements NbtFunction {
  public static final MapCodec<PickNbtFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(WeightedList.createMapCodec(NbtFunction.CODEC).forGetter(PickNbtFunction::functions)).apply(instance, PickNbtFunction::new));

  @Override
  public NbtFunctionType<PickNbtFunction> getType() {
    return NbtFunctionTypes.PICK;
  }

  @Override
  public String expressAsString() {
    return functions.asStringStream(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ", "pick(", ")"));
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final RandomSource random = context.random;
    return functions.getRandom(random).apply(nbtElement, context);
  }

  public static class Parser implements FunctionContentParser<NbtFunction> {
    private @Nullable WeightedList<NbtFunction> weightedList;

    @Override
    public NbtFunction getParseResult(ParseContext<?> parseContext) {
      return new PickNbtFunction(Objects.requireNonNull(weightedList, "weightedList"));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final WeightedListParser<NbtFunction> weightedListParser = WeightedListParser.of((parseContext1) -> NbtFunction.parse(parseContext, false, false));
      weightedList = weightedListParser.parse(parseContext);
    }
  }
}
