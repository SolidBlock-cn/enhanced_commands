package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import pers.solid.ecmd.util.FunctionParamsParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
public interface PickBlockFunction extends BlockFunction {
  SimpleCommandExceptionType SUM_ZERO = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.block_function.pick.zero_sum"));

  @Override
  default @NotNull BlockFunctionType<PickBlockFunction> getType() {
    return BlockFunctionTypes.PICK;
  }

  /**
   * 多个方块函数具有相等的权重。这种情况下可以最快地产生。
   */
  record Uniform(List<BlockFunction> functions) implements PickBlockFunction {
    public static final Codec<Uniform> CODEC = RecordCodecBuilder.create(i -> i.ap(Uniform::new, BlockFunction.CODEC.listOf().fieldOf("functions").forGetter(Uniform::functions)));

    @Override
    public @NotNull String asString() {
      return "pick(" + functions.stream().map(BlockFunction::asString).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      return functions.get(world.random.nextInt(functions.size())).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }

  }

  /**
   * 带有权重的方块函数，在运行时会根据权重来进行选择。
   */
  record Weighted(List<ObjectDoublePair<BlockFunction>> pairs) implements PickBlockFunction {
    public static final Codec<ObjectDoublePair<BlockFunction>> PAIR_CODEC = RecordCodecBuilder.create(j -> j.apply2(ObjectDoublePair::of, BlockFunction.CODEC.fieldOf("function").forGetter(ObjectDoublePair::left), Codec.DOUBLE.optionalFieldOf("probability", 1d).forGetter(ObjectDoublePair::rightDouble)));
    public static final Codec<Weighted> CODEC = RecordCodecBuilder.create(i -> i.ap(Weighted::new, PAIR_CODEC.listOf().fieldOf("pairs").forGetter(Weighted::pairs)));

    @Override
    public @NotNull String asString() {
      return "pick(" + pairs.stream().map(pair -> pair.left().asString() + " " + pair.rightDouble()).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      final Random random = world.getRandom();
      final double d = random.nextDouble();
      double stackedHeight = 0;

      // 注意：pairs 中的各浮点数的总和应该为 1。
      for (ObjectDoublePair<BlockFunction> pair : pairs) {
        stackedHeight += pair.rightDouble();
        if (d < stackedHeight) {
          return pair.left().getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
        }
      }

      return blockState;
    }

  }

  Codec<PickBlockFunction> CODEC = Codec.<PickBlockFunction, Uniform>either(Codec.BOOL.dispatch("weighted", f -> f instanceof Weighted, b -> b ? Weighted.CODEC : Uniform.CODEC), Uniform.CODEC).xmap(ei -> ei.map(Function.identity(), Function.identity()), Either::left);

  enum Type implements BlockFunctionType<PickBlockFunction> {
    PICK_TYPE;

    @Override
    public @NotNull Codec<PickBlockFunction> getCodec() {
      return CODEC;
    }
  }

  class Parser implements FunctionParamsParser<BlockFunctionArgument> {
    boolean weighted = false;
    final List<ObjectDoublePair<BlockFunctionArgument>> pairs = new ArrayList<>();

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException {
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
          return new Weighted(builder.build());
        };
      } else {
        return source -> {
          ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
          for (ObjectDoublePair<BlockFunctionArgument> pair : pairs) {
            builder.add(pair.left().apply(source));
          }
          return new Uniform(builder.build());
        };
      }
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final BlockFunctionArgument parse = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      parser.reader.skipWhitespace();
      if (parser.reader.canRead() && StringReader.isAllowedNumber(parser.reader.peek())) {
        final int cursorBeforeDouble = parser.reader.getCursor();
        final double weight = parser.reader.readDouble();
        if (weight < 0) {
          parser.reader.setCursor(cursorBeforeDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(parser.reader, 0, weight);
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
