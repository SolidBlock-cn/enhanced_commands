package pers.solid.ecmd.math;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.block.WeightedListParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import static pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension.withCursorEnd;
import static pers.solid.ecmd.util.ModCommandExceptionTypes.DUPLICATE_KEYWORD;

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
    x *= scale.x;
    y *= scale.y;
    z *= scale.z;
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
    protected Vec3d scale = null;
    protected Vec3d floor = null;
    protected Vec3d offset = null;
    protected int cursorBeforeFunctionName;
    public WeightedListParser<T> weightedListParser = WeightedListParser.of((registryAccess, parser, suggestionsOnly, allowSparse) -> parseElement(registryAccess, parser, suggestionsOnly));
    protected WeightedList<T> weightedList;

    @Override
    public void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
      this.cursorBeforeFunctionName = cursorBeforeFunctionName;
    }

    @Override
    public T getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
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
      parseEntryList(registryAccess, parser, suggestionsOnly);
      parser.addSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(rightParString()).suggest(";").buildFuture());
      // 等待关键字的部分

      // 解析坐标轴尺寸的部分
      parser.clearSuggestion();
      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();

        parseParameters(registryAccess, parser, reader, suggestionsOnly);
      }
    }

    protected void parseEntryList(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      weightedList = weightedListParser.parse(registryAccess, parser, suggestionsOnly, suggestionsOnly);
    }

    protected void parseParameters(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, StringReader reader, boolean suggestionsOnly) throws CommandSyntaxException {
      while (true) {
        parseParameter(registryAccess, parser, suggestionsOnly);

        parser.reader.skipWhitespace();
        parser.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(",").buildFuture());
        if (parser.reader.canRead() && parser.reader.peek() == ',') {
          parser.reader.skip();
          parser.reader.skipWhitespace();
        } else {
          break;
        }
      }
    }


    protected void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
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
      final String paramName = reader.readUnquotedString();
      final int cursorAfterKeyword = reader.getCursor();
      parser.setSuggestion((commandContext, suggestionsBuilder) -> {
        if (floor == null) ParsingUtil.suggestString("floor=", suggestionsBuilder);
        if (scale == null) ParsingUtil.suggestString("scale=", suggestionsBuilder);
        if (offset == null) ParsingUtil.suggestString("offset=", suggestionsBuilder);
        return suggestionsBuilder.buildFuture();
      });

      switch (paramName) {
        case "floor", "scale", "offset" -> {
          reader.skipWhitespace();
          parser.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest("=").buildFuture());
          reader.expect('=');
          reader.skipWhitespace();
        }
        default -> {
          reader.setCursor(cursorBeforeKeyword);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_KEYWORD.createWithContext(reader, paramName), cursorAfterKeyword);
        }
      }

      switch (paramName) {
        case "floor" -> {
          parser.clearSuggestion();
          if (floor != null) {
            reader.setCursor(cursorBeforeKeyword);
            throw withCursorEnd(DUPLICATE_KEYWORD.createWithContext(reader, paramName), cursorAfterKeyword);
          }
          ParsingUtil.expectAndSkipWhitespace(reader);
          floor = ParsingUtil.parseShortenableVec3d(reader);
        }
        case "scale" -> {
          if (scale != null) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, paramName), cursorAfterKeyword);
          }
          scale = ParsingUtil.parseShortenableVec3d(reader);
        }
        case "offset" -> {
          if (offset != null) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, paramName), cursorAfterKeyword);
          }
          offset = ParsingUtil.parseShortenableVec3d(reader);
        }
      }
    }
  }
}
