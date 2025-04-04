package pers.solid.ecmd.math;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.block.PickBlockFunction;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.ArrayList;
import java.util.List;

import static pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension.withCursorEnd;
import static pers.solid.ecmd.util.ModCommandExceptionTypes.DUPLICATE_KEYWORD;
import static pers.solid.ecmd.util.ModCommandExceptionTypes.UNKNOWN_KEYWORD;

/**
 * 用于棋盘格相关代码的共通接口。
 */
public interface Checkerboard<T> {
  Vec3d UNIT = new Vec3d(1, 1, 1);

  @NotNull
  Vec3d floor();

  @NotNull
  Vec3d scale();

  @NotNull
  Vec3d offset();

  default T getEntry(@NotNull WeightedList<T> entries, BlockPos pos) {
    final Vec3d offset = offset();
    final Vec3d floor = floor();
    final Vec3d scale = scale();
    double x = pos.getX() - offset.x;
    double y = pos.getY() - offset.y;
    double z = pos.getZ() - offset.z;
    if (floor.x != 0) x = Math.floor(x / floor.x);
    if (floor.y != 0) y = Math.floor(y / floor.y);
    if (floor.z != 0) z = Math.floor(z / floor.z);
    if (scale.x == 0) x = 0;
    else x /= scale.x;
    if (scale.y == 0) y = 0;
    else y /= scale.y;
    if (scale.z == 0) z = 0;
    else z /= scale.z;
    double v = x + y + z;
    return entries.getElementAt(v);
  }

  default StringBuilder appendParameters(StringBuilder sb) {
    final Vec3d floor = floor();
    final Vec3d scale = scale();
    final Vec3d offset = offset();
    if (!floor.equals(Vec3d.ZERO)) {
      sb.append(" floor ").append(StringUtil.wrapVector(floor));
    }
    if (!scale.equals(UNIT)) {
      sb.append(" scale ").append(StringUtil.wrapVector(scale));
    }
    if (!offset.equals(Vec3d.ZERO)) {
      sb.append(" offset ").append(offset);
    }
    return sb;
  }

  abstract class CheckerboardParser<T> implements FunctionLikeParser<T> {
    protected boolean weighted = false;
    protected Vec3d scale = null;
    protected Vec3d floor = null;
    protected double weightSum = 0;
    protected Vec3d offset = null;
    protected int cursorBeforeFunctionName;
    protected List<ObjectDoublePair<T>> pairs;

    @Override
    public void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
      this.cursorBeforeFunctionName = cursorBeforeFunctionName;
    }

    @Override
    public T getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      if (weightSum == 0) {
        final int cursorEnd = parser.reader.getCursor();
        parser.reader.setCursor(cursorBeforeFunctionName);
        throw withCursorEnd(PickBlockFunction.SUM_ZERO.createWithContext(parser.reader), cursorEnd);
      }
      if (scale == null) {
        scale = UNIT;
      }
      if (floor == null) {
        floor = Vec3d.ZERO;
      }
      if (offset == null) {
        offset = Vec3d.ZERO;
      }
      return getParseResult(floor, scale, offset);
    }

    protected abstract T getParseResult(Vec3d floor, Vec3d scale, Vec3d offset);

    protected abstract T parseElement(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException;

    @Override
    public void parseWithinParenthesis(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      parseEntryList(registryAccess, parser, suggestionsOnly, reader);
      // 等待关键字的部分

      // 解析坐标轴尺寸的部分
      parser.clearSuggestion();
      reader.skipWhitespace();

      parseParameters(parser, reader);
    }

    private void parseParameters(SuggestedParser<?> parser, StringReader reader) throws CommandSyntaxException {
      while (true) {
        parser.addSuggestion((context, suggestionsBuilder) -> {
          if (floor == null) {
            ParsingUtil.suggestString("floor", suggestionsBuilder);
          }
          if (scale == null) {
            ParsingUtil.suggestString("scale", suggestionsBuilder);
          }
          if (offset == null) {
            ParsingUtil.suggestString("offset", suggestionsBuilder);
          }
          return suggestionsBuilder.buildFuture();
        });

        final int cursorBeforeKeyword = reader.getCursor();
        final String keyword = reader.readUnquotedString();
        if (keyword.isEmpty()) {
          break;
        }
        final int cursorAfterKeyword = reader.getCursor();
        switch (keyword) {
          case "floor" -> {
            parser.clearSuggestion();
            if (floor != null) {
              reader.setCursor(cursorBeforeKeyword);
              throw withCursorEnd(DUPLICATE_KEYWORD.createWithContext(reader, keyword), cursorAfterKeyword);
            }
            ParsingUtil.expectAndSkipWhitespace(reader);
            floor = ParsingUtil.parseShortenableVec3d(reader);
          }
          case "scale" -> {
            parser.clearSuggestion();
            if (scale != null) {
              reader.setCursor(cursorBeforeKeyword);
              throw withCursorEnd(DUPLICATE_KEYWORD.createWithContext(reader, keyword), cursorAfterKeyword);
            }
            ParsingUtil.expectAndSkipWhitespace(reader);
            scale = ParsingUtil.parseShortenableVec3d(reader);
          }
          case "offset" -> {
            parser.clearSuggestion();
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

    protected void parseEntryList(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, StringReader reader) throws CommandSyntaxException {
      this.pairs = new ArrayList<>();

      if (reader.canRead() && reader.peek() == rightPar()) {
        throw FunctionParamsParser.PARAMS_TOO_FEW.createWithContext(reader, 0, 1);
      }

      // 解析方块函数的部分
      while (true) {
        parser.clearSuggestion();
        final T parse = parseElement(registryAccess, parser, suggestionsOnly);
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
        parser.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder
            .suggest(rightParString())
            .suggest(separatorString()).buildFuture());
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
    }
  }
}
