package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface CheckerboardBlockFunction extends BlockFunction {
  MapCodec<CheckerboardBlockFunction> CODEC = Codec.BOOL.dispatchMap("uniform", f -> f instanceof Uniform, u -> u ? Uniform.CODEC : Weighted.CODEC);

  @Override
  @NotNull
  default BlockFunctionType<?> getType() {
    return BlockFunctionTypes.CHECKERBOARD;
  }

  enum Type implements BlockFunctionType<CheckerboardBlockFunction> {
    CHECKERBOARD_TYPE;

    @Override
    public @NotNull MapCodec<CheckerboardBlockFunction> getCodec() {
      return CODEC;
    }
  }

  record Uniform(List<BlockFunction> functions, Vec3d axisScale, boolean floor) implements CheckerboardBlockFunction {
    public static final MapCodec<Uniform> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockFunction.CODEC.listOf().fieldOf("functions").forGetter(Uniform::functions),
        Vec3d.CODEC.fieldOf("axis_scale").forGetter(Uniform::axisScale),
        Codec.BOOL.optionalFieldOf("boolean", false).forGetter(Uniform::floor)).apply(i, Uniform::new));

    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      final double v = getPoint(floor, pos, axisScale);
      final int i = MathHelper.floorMod(MathHelper.floor(v), functions.size());
      return functions.get(i).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }

    @Override
    public @NotNull String asString() {
      return "checkerboard(" + functions.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + (floor ? " \\ " : " * ") + axisScale.x + " " + axisScale.y + " " + axisScale.z + ")";
    }
  }

  final class Weighted implements CheckerboardBlockFunction {
    public static final Codec<ObjectDoublePair<BlockFunction>> PAIR_CODEC = RecordCodecBuilder.create(i -> i.group(BlockFunction.CODEC.fieldOf("function").forGetter(ObjectDoublePair::left), Codec.DOUBLE.fieldOf("amount").forGetter(ObjectDoublePair::rightDouble)).apply(i, ObjectDoublePair::of));
    public static final MapCodec<Weighted> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(PAIR_CODEC.listOf().fieldOf("patterns").forGetter(Weighted::patterns),
        Vec3d.CODEC.fieldOf("axis_scale").forGetter(Weighted::axisScale),
        Codec.BOOL.optionalFieldOf("floor", false).forGetter(Weighted::floor)).apply(i, Weighted::new));

    public List<ObjectDoublePair<BlockFunction>> patterns() {
      return patterns;
    }

    public Vec3d axisScale() {
      return axisScale;
    }

    public boolean floor() {
      return floor;
    }

    private final List<ObjectDoublePair<BlockFunction>> patterns;
    private final Vec3d axisScale;
    public final double totalLength;
    private final boolean floor;

    public Weighted(List<ObjectDoublePair<BlockFunction>> patterns, Vec3d axisScale, boolean floor) {
      this.patterns = patterns;
      this.axisScale = axisScale;
      this.totalLength = patterns.stream().mapToDouble(ObjectDoublePair::rightDouble).sum();
      this.floor = floor;
    }

    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      final double v = getPoint(floor, pos, axisScale);
      final double i = MathHelper.floorMod(v, totalLength);
      double stackedHeight = 0;

      // 注意：pairs 中的各浮点数的总和应该为 1。
      for (ObjectDoublePair<BlockFunction> pair : patterns) {
        stackedHeight += pair.rightDouble();
        if (i < stackedHeight) {
          return pair.left().getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
        }
      }

      return blockState;
    }

    @Override
    public @NotNull String asString() {
      return "checkerboard(" + patterns.stream().map(pair -> pair.left().asString() + " " + pair.rightDouble()) + (floor ? " \\ " : " * ") + axisScale.x + " " + axisScale.y + " " + axisScale.z + ")";
    }
  }

  private static double getPoint(boolean floor, BlockPos pos, Vec3d axisScale) {
    final double v;
    if (floor) {
      v = Math.floor(pos.getX() / axisScale.x) + Math.floor(pos.getY() / axisScale.y) + Math.floor(pos.getZ() / axisScale.z);
    } else {
      v = Vec3d.of(pos).dotProduct(axisScale);
    }
    return v;
  }

  class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    protected final List<ObjectDoublePair<BlockFunctionArgument>> pairs = new ArrayList<>();
    protected boolean weighted = false;
    protected Vec3d axisScale = null;
    protected boolean shouldDivide = false;
    protected boolean shouldFloor = false;
    private double weightSum = 0;

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException {
      if (axisScale == null) {
        axisScale = new Vec3d(1, 1, 1);
      }
      if (weighted) {
        return source -> new Weighted(IterateUtils.transformFailableImmutableList(pairs, pair -> ObjectDoublePair.of(pair.left().apply(source), pair.rightDouble())), axisScale, shouldFloor);
      } else {
        return source -> new Uniform(IterateUtils.transformFailableImmutableList(pairs, pair -> pair.left().apply(source)), axisScale, shouldFloor);
      }
    }

    @Override
    public void parseWithinParenthesis(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      if (reader.canRead() && reader.peek() == rightPar()) {
        throw FunctionParamsParser.PARAMS_TOO_FEW.createWithContext(reader, 0, 1);
      }

      // 解析方块函数的部分
      while (true) {
        parser.suggestionProviders.clear();
        final BlockFunctionArgument parse = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
        reader.skipWhitespace();
        if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          final int cursorBeforeDouble = reader.getCursor();
          final double weight = reader.readDouble();
          final int cursorAfterDouble = reader.getCursor();
          if (weight < 0) {
            reader.setCursor(cursorBeforeDouble);
            throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, weight), cursorAfterDouble);
          }
          weightSum += weight;
          if (weightSum == 0) {
            reader.setCursor(cursorBeforeDouble);
            throw CommandSyntaxExceptionExtension.withCursorEnd(PickBlockFunction.SUM_ZERO.createWithContext(reader), cursorAfterDouble);
          }
          weighted = true;
          pairs.add(ObjectDoublePair.of(parse, weight));
        } else {
          pairs.add(ObjectDoublePair.of(parse, 1));
          weightSum += 1;
        }

        reader.skipWhitespace();
        parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestionsBuilder
            .suggest(rightParString())
            .suggest(separatorString())
            .suggest("X")
            .suggest("/")
            .suggest("\\"));

        if (!reader.canRead()) {
          throw ModCommandExceptionTypes.EXPECTED_4_SYMBOLS.createWithContext(reader, rightParString(), "X", "/", "\\");
        }
        final char peek = reader.peek();
        if (peek == ',') {
          reader.skip();
          reader.skipWhitespace();
        } else if (peek == 'X' || peek == 'x' || peek == '/' || peek == '\\') {
          reader.skip();
          shouldDivide = peek == '/';
          shouldFloor = peek == '\\';
          break;
        } else if (peek == rightPar()) {
          return;
        }
      }

      // 解析坐标轴尺寸的部分
      parser.suggestionProviders.clear();
      reader.skipWhitespace();
      double x = reader.readDouble();
      ParsingUtil.expectAndSkipWhitespace(reader);
      double y = reader.readDouble();
      ParsingUtil.expectAndSkipWhitespace(reader);
      double z = reader.readDouble();
      if (shouldDivide) {
        this.axisScale = new Vec3d(1d / x, 1d / y, 1d / z);
      } else {
        this.axisScale = new Vec3d(x, y, z);
      }
    }
  }
}
