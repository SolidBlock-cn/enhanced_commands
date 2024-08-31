package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.WeightedList;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

import java.util.ArrayList;
import java.util.List;
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
public record PickBlockFunction(WeightedList<BlockFunction> functions) implements BlockFunction {
  public static final SimpleCommandExceptionType SUM_ZERO = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.block_function.pick.zero_sum"));
  public static final MapCodec<PickBlockFunction> CODEC = WeightedList.createMapCodec(BlockFunction.CODEC).xmap(PickBlockFunction::new, PickBlockFunction::functions);

  @Override
  @NotNull
  public BlockFunctionType<PickBlockFunction> getType() {
    return BlockFunctionTypes.PICK;
  }

  enum Type implements BlockFunctionType<PickBlockFunction> {
    PICK_TYPE;

    @Override
    public @NotNull MapCodec<PickBlockFunction> getCodec() {
      return CODEC;
    }
  }


    @Override
    public @NotNull String asString() {
      return functions.asStringStream(ExpressionConvertible::asString).collect(Collectors.joining(",", "pick(", ")"));
    }


    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      final Random random = world.getRandom();
      return functions.getRandom(random).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }


  public static class Parser implements FunctionParamsParser<BlockFunctionArgument> {
    final List<ObjectDoublePair<BlockFunctionArgument>> pairs = new ArrayList<>();
    boolean weighted = false;

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      if (weighted) {
        final double sum = pairs.stream().mapToDouble(ObjectDoublePair::rightDouble).sum();
        if (sum == 0) {
          throw SUM_ZERO.createWithContext(parser.reader);
        }
        return source -> {
          ImmutableList.Builder<ObjectDoublePair<BlockFunction>> builder = new ImmutableList.Builder<>();
          for (ObjectDoublePair<BlockFunctionArgument> pair : pairs) {
            builder.add(ObjectDoublePair.of(pair.left().apply(source), pair.rightDouble() / sum));
          }
          return new PickBlockFunction(new WeightedList.Weighted<>(builder.build()));
        };
      } else {
        return source -> {
          ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
          for (ObjectDoublePair<BlockFunctionArgument> pair : pairs) {
            builder.add(pair.left().apply(source));
          }
          return new PickBlockFunction(new WeightedList.Uniform<>(builder.build()));
        };
      }
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final BlockFunctionArgument parse = BlockFunctionArgument.parse(registryAccess, parser, suggestionsOnly);
      parser.reader.skipWhitespace();
      if (parser.reader.canRead() && StringReader.isAllowedNumber(parser.reader.peek())) {
        final int cursorBeforeDouble = parser.reader.getCursor();
        final double weight = parser.reader.readDouble();
        final int cursorAfterDouble = parser.reader.getCursor();
        if (weight < 0) {
          parser.reader.setCursor(cursorBeforeDouble);
          throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(parser.reader, 0, weight), cursorAfterDouble);
        }
        weighted = true;
        pairs.add(ObjectDoublePair.of(parse, weight));
      } else {
        pairs.add(ObjectDoublePair.of(parse, 1));
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }
  }
}
