package pers.solid.ecmd.math;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.function.block.WeightedListParser;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.NamedParamListParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collection;
import java.util.Set;

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

  abstract class CheckerboardParser<T> implements FunctionLikeParser<T>, NamedParamListParser {
    protected final Set<String> SUPPORTED_PARAMS = Set.of("scale", "floor", "offset");
    public final WeightedListParser<T> weightedListParser = WeightedListParser.of(this::parseElement);
    protected Vec3d scale = null;
    protected Vec3d floor = null;
    protected Vec3d offset = null;
    protected int cursorBeforeFunctionName;
    protected WeightedList<T> weightedList;

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
      this.cursorBeforeFunctionName = cursorBeforeFunctionName;
    }

    @Override
    public T getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
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

    protected abstract T parseElement(ParseContext<?> parseContext) throws CommandSyntaxException;

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      parseEntryList(parseContext);
      parseContext.addSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(rightParString()).suggest(";").buildFuture());
      // 等待关键字的部分

      // 解析坐标轴尺寸的部分
      parseContext.clearSuggestion();
      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        parseNamedParameters(parseContext);
      }
    }

    protected void parseEntryList(ParseContext<?> parseContext) throws CommandSyntaxException {
      weightedList = weightedListParser.parse(parseContext);
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return switch (paramName) {
        case "floor" -> floor != null;
        case "scale" -> scale != null;
        case "offset" -> offset != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();

      switch (paramName) {
        case "floor" -> floor = ParsingUtil.parseShortenableVec3d(reader);
        case "scale" -> scale = ParsingUtil.parseShortenableVec3d(reader);
        case "offset" -> offset = ParsingUtil.parseShortenableVec3d(reader);
      }
    }
  }
}
