package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.*;

import java.util.Iterator;
import java.util.function.Function;

/**
 * 完整的圆或者仅一个圆弧。语法规则：
 * <table>
 *   <tr><th>代码<th>描述
 *   <tr><td>{@code circle(<radius>, <center>[ ranging <range>])}
 *    <td>绕 y 轴正方向单位向量，从 x 轴正方向单位向量旋转
 *   <tr><td>{@code circle(<radius>, <center> around <axis>)}
 *    <td>绕指定坐标轴正方向单位向量，从另一坐标轴正方向单位向量旋转完整一周
 *   <tr><td>{@code circle(from [positive|negative] <radiusAxis>, <center> around [positive|negative] <axis> [ranging <range>])}
 *    <td>绕指定坐标轴方向的单位向量，从另一坐标轴单位向量旋转。
 *   <tr><td>{@code circle(from <vec>, <center> around <vec> [ranging <range>])}
 *    <td>绕指定向量，从另一向量开始旋转。
 * </table>
 *
 * @param radius 圆的半径，是一个相对向量。
 * @param center 圆的中心。
 * @param axis   旋转轴，通常应该要和 {@code radius} 垂直。
 * @param range  弧的范围，使用弧度制。完整的圆即为 0 到 2π。
 */
public record CircleCurve(Vec3d radius, Vec3d center, Vec3d axis, @NotNull Range<Double> range) implements Curve {
  public static final Range<Double> FULL_TURN_RANGE = Range.between(0d, Math.PI * 2d);

  @Override
  public @NotNull Iterator<Vec3d> iteratePoints(Number interval) {
    return new AbstractIterator<>() {
      final double c = 2 * Math.PI * radius.length();
      private final Vector3d radiusVec = new Vector3d(radius.x, radius.y, radius.z);
      private final AxisAngle4d axisAngle4d = new AxisAngle4d(range.getMinimum(), axis.x, axis.y, axis.z);

      @Override
      protected Vec3d computeNext() {
        if (axisAngle4d.angle > range.getMaximum()) {
          return endOfData();
        }
        radiusVec.set(radius.x, radius.y, radius.z);
        axisAngle4d.transform(radiusVec);
        axisAngle4d.angle += interval.doubleValue() / c;
        return new Vec3d(radiusVec.x + center.x, radiusVec.y + center.y, radiusVec.z + center.z);
      }
    };
  }

  @Override
  public double length() {
    return radius.length() * (range.getMaximum() - range.getMinimum());
  }

  @Override
  public @NotNull Curve moved(@NotNull Vec3d relativePos) {
    return new CircleCurve(radius, center.add(relativePos), axis, range);
  }

  @Override
  public @NotNull Curve rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return new CircleCurve(GeoUtil.rotate(radius, blockRotation, Vec3d.ZERO), GeoUtil.rotate(center, blockRotation, pivot), GeoUtil.rotate(axis, blockRotation, Vec3d.ZERO), range);
  }

  @Override
  public @NotNull Curve mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return new CircleCurve(GeoUtil.mirror(radius, axis, Vec3d.ZERO), GeoUtil.mirror(center, axis, pivot), GeoUtil.mirror(this.axis, axis, Vec3d.ZERO), range);
  }

  @Override
  public @NotNull String asString() {
    if (axis.subtract(0, 1, 0).equals(Vec3d.ZERO)) {
      // axis 是 y 轴上的单位向量
      if (radius.subtract(1, 0, 0).equals(Vec3d.ZERO)) {
        if (range.equals(FULL_TURN_RANGE)) {
          // 表示一个最简单的旋转，绕 y 轴正方向，从 x 正方向开始旋转一周
          return "circle(%s at %s)".formatted(radius.y, StringUtil.wrapPosition(center));
        } else {
          // 表示绕 y 轴正方向，从 x 正方向开始旋转一个特定的范围
          return "circle(%s at %s ranging %s)".formatted(radius.y, StringUtil.wrapPosition(center), wrapRadRange(range));
        }
      }
    }
    if (range.equals(FULL_TURN_RANGE)) {
      // 这种情况下，由于本来就是旋转一整周，因此没有必要指定开始坐标。
      if (radius.dotProduct(axis) == 0) {
        // 半径向量和轴向量垂直。
        return "circle(%s at %s around %s)".formatted(radius.length(), StringUtil.wrapPosition(center), wrapVector(axis));
      } else {
        return "circle(from %s at %s around %s)".formatted(wrapRadius(radius), StringUtil.wrapPosition(center), wrapVector(axis));
      }
    } else {
      return "circle(from %s at %s around %s ranging %s)".formatted(wrapRadius(radius), StringUtil.wrapPosition(center), wrapVector(axis), wrapRadRange(range));
    }
  }

  @Override
  public @NotNull CurveType<CircleCurve> getCurveType() {
    return Type.INSTANCE;
  }

  public static String wrapRadRange(Range<? extends Number> range) {
    if (range.getMinimum().doubleValue() == 0) {
      return range.getMaximum().toString() + "rad";
    } else {
      return range.getMinimum().toString() + "rad.." + range.getMinimum().toString() + "rad";
    }
  }

  public static String wrapVector(Vec3d vec3d) {
    if (vec3d.multiply(0, 1, 1).equals(Vec3d.ZERO)) {
      if (vec3d.x == 1) return "positive x";
      else if (vec3d.x == -1) return "negative x";
    } else if (vec3d.multiply(1, 0, 1).equals(Vec3d.ZERO)) {
      if (vec3d.y == 1) return "positive y";
      else if (vec3d.y == -1) return "negative y";
    } else if (vec3d.multiply(1, 1, 0).equals(Vec3d.ZERO)) {
      if (vec3d.z == 1) return "positive z";
      else if (vec3d.z == -1) return "negative z";
    }
    return StringUtil.wrapPosition(vec3d);
  }

  public static String wrapRadius(Vec3d vec3d) {
    if (vec3d.multiply(0, 1, 1).equals(Vec3d.ZERO)) {
      if (vec3d.x > 0) return "positive x " + vec3d.x;
      else if (vec3d.x < 0) return "negative x " + -vec3d.x;
    } else if (vec3d.multiply(1, 0, 1).equals(Vec3d.ZERO)) {
      if (vec3d.y > 0) return "positive y " + vec3d.y;
      else if (vec3d.y < 0) return "negative y " + -vec3d.y;
    } else if (vec3d.multiply(1, 1, 0).equals(Vec3d.ZERO)) {
      if (vec3d.z > 0) return "positive z " + vec3d.z;
      else if (vec3d.z < 0) return "negative z " + -vec3d.z;
    }
    return StringUtil.wrapPosition(vec3d);
  }

  public enum Type implements CurveType<CircleCurve> {
    INSTANCE;

    @Override
    public @Nullable CurveArgument<CircleCurve> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  /**
   * 语法：
   * <pre>{@code
   *   circle(<radius> [at <center>] <=> [ranging <range>] <=> [around <axis>])
   *   circle(from <radiusVec> [at <center>] <=> [around <axis>] <=> [ranging <range>])
   *
   *   <radiusVec> = ([positive|negative] x|y|z <radius: double>) | <vec>
   *   <axis> = ([positive|negative] x|y|z) | <vec>
   *   <range> = <angle>[..<angle>]
   *   <angle> = <int>deg|<int>turn|<int>rad
   * }</pre>
   * <p>
   * 其中：{@code <range>} 的默认值为 {@code 0turn..1turn}，{@code <axis>} 的默认值为 {@code positive y}。{@code [positive|negative]} 未指定时，默认为 {@code positive}。当 {@code <radius>} 指定为标量时，其方向相当于 {@code <axis>} 与 {@code positive y} 的向量积的方向。当 {@code <axis>} 正好指定为 {@code positive y} 方向时，{@code <radius>} 方向为 x 正方向，或为 {@code negative y} 方向，则 {@code <radius>} 方向为 x 负方向。
   */
  private static class Parser implements FunctionLikeParser<CurveArgument<CircleCurve>> {
    private @Nullable Either<Double, Vec3d> radius;
    private @Nullable PosArgument center;
    private @Nullable Vec3d around;
    private @Nullable Range<Double> range;
    private static final EnhancedPosArgumentType VECTOR_POS_ARGUMENT = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.DOUBLE_ONLY, true);

    @Override
    public @NotNull String functionName() {
      return "circle";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.curve.circle");
    }

    @Override
    public CurveArgument<CircleCurve> getParseResult(SuggestedParser parser) throws CommandSyntaxException {
      final Vec3d around = this.around == null ? new Vec3d(0, 1, 0) : this.around.normalize();
      if (this.radius == null) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, "radius");
      }
      final Vec3d radius = this.radius.map(d -> {
        Vec3d crossProduct = around.crossProduct(new Vec3d(0, 1, 0));
        if (crossProduct.equals(Vec3d.ZERO)) {
          crossProduct = around.y >= 0 ? new Vec3d(1, 0, 0) : new Vec3d(-1, 0, 0);
        } else {
          crossProduct = crossProduct.multiply(1 / crossProduct.length());
        }
        return crossProduct.multiply(d);
      }, Function.identity());
      final PosArgument center = this.center == null ? EnhancedPosArgumentType.HERE_INT : this.center;
      return source -> new CircleCurve(radius, center.toAbsolutePos(source), around, range == null ? FULL_TURN_RANGE : range);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      parseRadius(parser, suggestionsOnly);
      while (true) {
        final int cursorBeforeWhite = parser.reader.getCursor();
        parser.reader.skipWhitespace();
        if (parser.reader.getCursor() > cursorBeforeWhite) {
          parseAdditionalParameters(parser, suggestionsOnly);
        } else {
          break;
        }
      }
    }

    /**
     * 解析半径。即：{@code <double> | from (<vector> | [positive|negative] (x|y|z) <double>)}。
     */
    private void parseRadius(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final int cursorBeforeKeyword = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      parser.suggestionProviders.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("from", suggestionsBuilder));
      if ("from".equals(unquotedString)) {
        StringUtil.expectAndSkipWhitespace(reader);
        // TODO: 2023年9月20日 to accept axis names
        radius = Either.right(parseVector(parser, suggestionsOnly));
      } else {
        reader.setCursor(cursorBeforeKeyword);
        if (!CommandSource.shouldSuggest(reader.getRemaining(), "from")) parser.suggestionProviders.clear();

        radius = Either.left(reader.readDouble());
      }
    }

    private void parseAdditionalParameters(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestionProviders.clear();
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (center == null) SuggestionUtil.suggestString("at", suggestionsBuilder);
        if (around == null) SuggestionUtil.suggestString("around", suggestionsBuilder);
        if (range == null) SuggestionUtil.suggestString("ranging", suggestionsBuilder);
      });
      final int cursorBeforeKeyword = parser.reader.getCursor();
      final String unquotedString = parser.reader.readUnquotedString();
      if ("at".equals(unquotedString)) {
        if (center != null) {
          parser.reader.setCursor(cursorBeforeKeyword);
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parser.reader, unquotedString);
        }
        parser.suggestionProviders.clear();
        StringUtil.expectAndSkipWhitespace(parser.reader);
        center = SuggestionUtil.suggestParserFromType(new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false), parser, suggestionsOnly);
      } else if ("around".equals(unquotedString)) {
        if (around != null) {
          parser.reader.setCursor(cursorBeforeKeyword);
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parser.reader, unquotedString);
        }
        parser.suggestionProviders.clear();
        StringUtil.expectAndSkipWhitespace(parser.reader);
        // TODO: 2023年9月20日 to accept direction names
        around = parseVector(parser, suggestionsOnly);
      } else if ("ranging".equals(unquotedString)) {
        if (range != null) {
          parser.reader.setCursor(cursorBeforeKeyword);
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parser.reader, unquotedString);
        }
        parser.suggestionProviders.clear();
        StringUtil.expectAndSkipWhitespace(parser.reader);
        range = parseAngleRange(parser);
      } else {
        parser.reader.setCursor(cursorBeforeKeyword);
        throw ModCommandExceptionTypes.UNKNOWN_KEYWORD.createWithContext(parser.reader, unquotedString);
      }
    }

    private Vec3d parseVector(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final PosArgument posArgument = SuggestionUtil.suggestParserFromType(VECTOR_POS_ARGUMENT, parser, suggestionsOnly);
      return posArgument.toAbsolutePos(new ServerCommandSource(null, Vec3d.ZERO, Vec2f.ZERO, null, 0, null, null, null, null));
    }

    private Range<Double> parseAngleRange(SuggestedParser parser) throws CommandSyntaxException {
      final double firstAngle = SuggestionUtil.parseAngle(parser);
      parser.suggestionProviders.clear();
      final StringReader reader = parser.reader;
      final int cursorBeforeWhitespace = reader.getCursor();
      reader.skipWhitespace();
      if (reader.getString().startsWith("..", reader.getCursor())) {
        reader.setCursor(reader.getCursor() + "..".length());
        reader.skipWhitespace();
        final double secondAngle = SuggestionUtil.parseAngle(parser);
        parser.suggestionProviders.clear();
        return Range.between(firstAngle, secondAngle);
      } else {
        reader.setCursor(cursorBeforeWhitespace);
        return Range.between(0d, firstAngle);
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }
  }
}
