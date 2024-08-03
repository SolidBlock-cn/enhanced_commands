package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.WeightedList;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.ArrayList;
import java.util.List;

import static pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension.withCursorEnd;
import static pers.solid.ecmd.util.ModCommandExceptionTypes.DUPLICATE_KEYWORD;
import static pers.solid.ecmd.util.ModCommandExceptionTypes.UNKNOWN_KEYWORD;

public record CheckerboardBlockFunction(@NotNull WeightedList<BlockFunction> functions, @NotNull Vec3d floor, @NotNull Vec3d axisScale, @NotNull Vec3d offset) implements BlockFunction {
  private static final Vec3d UNIT = new Vec3d(1, 1, 1);
  public static final MapCodec<CheckerboardBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("functions").forGetter(CheckerboardBlockFunction::functions),
      Vec3d.CODEC.optionalFieldOf("floor", Vec3d.ZERO).forGetter(CheckerboardBlockFunction::floor),
      Vec3d.CODEC.optionalFieldOf("axis_scale", UNIT).forGetter(CheckerboardBlockFunction::axisScale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(CheckerboardBlockFunction::offset)
  ).apply(i, CheckerboardBlockFunction::new));

  @Override
  @NotNull
  public BlockFunctionType<?> getType() {
    return BlockFunctionTypes.CHECKERBOARD;
  }

  public enum Type implements BlockFunctionType<CheckerboardBlockFunction> {
    CHECKERBOARD_TYPE;

    @Override
    public @NotNull MapCodec<CheckerboardBlockFunction> getCodec() {
      return CODEC;
    }
  }
    @Override
    public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
      double x = pos.getX() - offset.x;
      double y = pos.getY() - offset.y;
      double z = pos.getZ() - offset.z;
      if (floor.x != 0) x = Math.floor(x / floor.x);
      if (floor.y != 0) y = Math.floor(y / floor.y);
      if (floor.z != 0) z = Math.floor(z / floor.z);
      if (axisScale.x == 0) x = 0;
      else x /= axisScale.x;
      if (axisScale.y == 0) y = 0;
      else y /= axisScale.y;
      if (axisScale.z == 0) z = 0;
      else z /= axisScale.z;
      double v = x + y + z;
      return functions.getElementAt(v).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }

    @Override
    public @NotNull String asString() {
      final StringBuilder sb = new StringBuilder("checkerboard");
      sb.append(functions.asString(ExpressionConvertible::asString));
      if (!floor.equals(Vec3d.ZERO)) {
        sb.append(" floor ").append(StringUtil.wrapPosition(floor));
      }
      if (!axisScale.equals(UNIT)) {
        sb.append(" scale ").append(StringUtil.wrapPosition(axisScale));
      }
      if (!offset.equals(Vec3d.ZERO)) {
        sb.append(" offset ").append(offset);
      }
      return sb.append(")").toString();
    }

  public static class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    protected final List<ObjectDoublePair<BlockFunctionArgument>> pairs = new ArrayList<>();
    protected boolean weighted = false;
    protected Vec3d axisScale = null;
    protected Vec3d floor = null;
    private double weightSum = 0;
    private Vec3d offset = null;
    private int cursorBeforeFunctionName;

    @Override
    public void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
      this.cursorBeforeFunctionName = cursorBeforeFunctionName;
    }

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException {
      if (weightSum == 0) {
        final int cursorEnd = parser.reader.getCursor();
        parser.reader.setCursor(cursorBeforeFunctionName);
        throw withCursorEnd(PickBlockFunction.SUM_ZERO.createWithContext(parser.reader), cursorEnd);
      }
      if (axisScale == null) {
        axisScale = UNIT;
      }
      if (floor == null) {
        floor = Vec3d.ZERO;
      }
      if (offset == null) {
        offset = Vec3d.ZERO;
      }
      if (weighted) {
        return source -> new CheckerboardBlockFunction(new WeightedList.Weighted<>(IterateUtils.transformFailableImmutableList(pairs, pair -> ObjectDoublePair.of(pair.left().apply(source), pair.rightDouble()))), floor, axisScale, offset);
      } else {
        return source -> new CheckerboardBlockFunction(new WeightedList.Uniform<>(IterateUtils.transformFailableImmutableList(pairs, pair -> pair.left().apply(source))), floor, axisScale, offset);
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
            throw withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, weight), cursorAfterDouble);
          }
          weightSum += weight;
          weighted |= weight != 1;
          pairs.add(ObjectDoublePair.of(parse, weight));
        } else {
          pairs.add(ObjectDoublePair.of(parse, 1));
          weightSum += 1;
        }

        reader.skipWhitespace();
        parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestionsBuilder
            .suggest(rightParString())
            .suggest(separatorString()));
        if (!reader.canRead()) {
          break;
        }
        reader.skipWhitespace();
        final char peek = reader.peek();
        if (peek == ',') {
          reader.skip();
          reader.skipWhitespace();
        } else {
          break;
        }
      }
      // 等待关键字的部分

      // 解析坐标轴尺寸的部分
      parser.suggestionProviders.clear();
      reader.skipWhitespace();

      while (true) {
        parser.suggestionProviders.add((context, suggestionsBuilder) -> {
          if (floor == null) {
            ParsingUtil.suggestString("floor", suggestionsBuilder);
          }
          if (axisScale == null) {
            ParsingUtil.suggestString("scale", suggestionsBuilder);
          }
          if (offset == null) {
            ParsingUtil.suggestString("offset", suggestionsBuilder);
          }
        });

        final int cursorBeforeKeyword = reader.getCursor();
        final String keyword = reader.readUnquotedString();
        if (keyword.isEmpty()) {
          break;
        }
        final int cursorAfterKeyword = reader.getCursor();
        switch (keyword) {
          case "floor" -> {
            parser.suggestionProviders.clear();
            if (floor != null) {
              reader.setCursor(cursorBeforeKeyword);
              throw withCursorEnd(DUPLICATE_KEYWORD.createWithContext(reader, keyword), cursorAfterKeyword);
            }
            ParsingUtil.expectAndSkipWhitespace(reader);
            floor = ParsingUtil.parseShortenableVec3d(reader);
          }
          case "scale" -> {
            parser.suggestionProviders.clear();
            if (axisScale != null) {
              reader.setCursor(cursorBeforeKeyword);
              throw withCursorEnd(DUPLICATE_KEYWORD.createWithContext(reader, keyword), cursorAfterKeyword);
            }
            ParsingUtil.expectAndSkipWhitespace(reader);
            axisScale = ParsingUtil.parseShortenableVec3d(reader);
          }
          case "offset" -> {
            parser.suggestionProviders.clear();
            if (offset != null) {
              reader.setCursor(cursorBeforeKeyword);
              throw withCursorEnd(DUPLICATE_KEYWORD.createWithContext(reader, keyword), cursorAfterKeyword);
            }
            ParsingUtil.expectAndSkipWhitespace(reader);
            offset = ParsingUtil.parseShortenableVec3d(reader);
          }
          default -> {
            reader.setCursor(cursorBeforeKeyword);
            throw withCursorEnd(UNKNOWN_KEYWORD.createWithContext(reader, keyword), cursorAfterKeyword);
          }
        } // end switch

        reader.skipWhitespace();
      }
    }
  }
}
