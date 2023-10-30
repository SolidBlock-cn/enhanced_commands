package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

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
public interface PickBlockFunction extends BlockFunction {
  SimpleCommandExceptionType SUM_ZERO = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_function.pick.zero_sum"));

  @Override
  default @NotNull BlockFunctionType<PickBlockFunction> getType() {
    return BlockFunctionTypes.PICK;
  }

  /**
   * 多个方块函数具有相等的权重。这种情况下可以最快地产生。
   */
  record Uniform(List<BlockFunction> blockFunctions) implements PickBlockFunction {
    @Override
    public @NotNull String asString() {
      return "pick(" + blockFunctions.stream().map(BlockFunction::asString).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      return blockFunctions.get(world.random.nextInt(blockFunctions.size())).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }

    @Override
    public void writeNbt(@NotNull NbtCompound nbtCompound) {
      nbtCompound.putBoolean("weighted", false);
      final NbtList nbtList = new NbtList();
      nbtCompound.put("functions", nbtList);
      nbtList.addAll(Lists.transform(blockFunctions, BlockFunction::createNbt));
    }
  }

  /**
   * 带有权重的方块函数，在运行时会根据权重来进行选择。
   */
  record Weighted(List<ObjectDoublePair<BlockFunction>> pairs) implements PickBlockFunction {
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

    @Override
    public void writeNbt(@NotNull NbtCompound nbtCompound) {
      nbtCompound.putBoolean("weighted", true);
      final NbtList nbtList = new NbtList();
      nbtCompound.put("functions", nbtList);
      for (ObjectDoublePair<BlockFunction> pair : pairs) {
        final NbtCompound nbtCompound1 = new NbtCompound();
        nbtList.add(nbtCompound1);
        nbtCompound1.put("value", pair.left().createNbt());
        nbtCompound1.putDouble("weight", pair.rightDouble());
      }
    }
  }

  enum Type implements BlockFunctionType<PickBlockFunction> {
    PICK_TYPE;

    @Override
    public @NotNull PickBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final boolean weighted = nbtCompound.getBoolean("weighted");
      if (weighted) {
        final ImmutableList.Builder<ObjectDoublePair<BlockFunction>> pairsBuilder = new ImmutableList.Builder<>();
        final NbtList nbtList = nbtCompound.getList("functions", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : nbtList) {
          if (!(element instanceof final NbtCompound nbtCompound1))
            continue;
          final float weight = nbtCompound1.getFloat("weight");
          final BlockFunction value = BlockFunction.fromNbt(nbtCompound1.getCompound("value"), world);
          pairsBuilder.add(ObjectDoublePair.of(value, weight));
        }
        return new Weighted(pairsBuilder.build());
      } else {
        final NbtList nbtList = nbtCompound.getList("functions", NbtElement.COMPOUND_TYPE);
        return new Uniform(nbtList.stream().map(nbtElement -> BlockFunction.fromNbt(((NbtCompound) nbtElement), world)).collect(ImmutableList.toImmutableList()));
      }
    }

    @Override
    public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    boolean weighted = false;
    final List<ObjectDoublePair<BlockFunctionArgument>> pairs = new ArrayList<>();

    @Override
    public @NotNull String functionName() {
      return "pick";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.block_function.pick");
    }

    @Override
    public BlockFunctionArgument getParseResult(SuggestedParser parser) throws CommandSyntaxException {
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
