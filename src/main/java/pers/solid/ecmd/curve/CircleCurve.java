package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.*;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * <p>完整的圆或者仅一个圆弧。语法规则：
 * <table>
 *   <tr><th>代码<th>描述
 *   <tr><td>{@code circle(<radius> [at <center>] [ranging <range>])}
 *    <td>绕 y 轴正方向单位向量，从 x 轴正方向单位向量旋转
 *   <tr><td>{@code circle(<radius> [at <center>] [around <axis>])}
 *    <td>绕指定坐标轴正方向单位向量，从另一坐标轴正方向单位向量旋转完整一周
 *   <tr><td>{@code circle(from <radiusVector> [at <center>] [around <axisVector>] [ranging <range>])}
 *    <td>绕指定坐标轴方向的单位向量，从另一坐标轴单位向量旋转。
 *   <tr><td>{@code circle(from <radiusVector> [at <center>] [around <axisVector>] [ranging <range>])}
 *    <td>绕指定向量，从另一向量开始旋转。
 * </table>
 *
 * <p>{@code around <axisVector>} 等价于 {@code rotated <x> <y>} 或 {@code facing <targetPos>}。所有轴向量都会被单位化。
 * <p>其中，{@code <radiusVector>} 和 {@code <axisVector>} 的解析方式为 {@code <x> <y> <z>} 或 {@code [length] <direction>}（参照 {@link ParsingUtil#parseVec3d(SuggestedParser)}。
 * <p>当半径指定为标量时，其方向为轴向量与 y 轴正方向的向量积的方向，若轴向量为 y 轴正方向或负方向，则其方向为 x 轴正方向或负方向。
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
    return StringUtil.wrapPosition(vec3d);
  }

  public static String wrapRadius(Vec3d vec3d) {
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
   *   <radiusVec> = (<radius: double> <direction>) | <vec>
   *   <axis> = <direction> | <vec>
   *   <range> = <angle>[..<angle>]
   *   <angle> = <int>deg|<int>turn|<int>rad
   * }</pre>
   * <p>
   * 其中：{@code <range>} 的默认值为 {@code 0turn..1turn}，{@code <axis>} 的默认值为 {@code 0 1 0}。当 {@code <radius>} 指定为标量时，其方向相当于 {@code <axis>} 与 y 轴正方向的向量积的方向。当 {@code <axis>} 正好指定为 y 轴正方向时，{@code <radius>} 方向为 x 正方向，若为 y 轴负方向，则 {@code <radius>} 方向为 x 负方向。
   */
  private static class Parser implements FunctionLikeParser<CurveArgument<CircleCurve>> {
    private @Nullable Either<Double, Vec3d> radius;
    private @Nullable PosArgument center;
    private @Nullable FailableBiFunction<ServerCommandSource, Vec3d, Vec3d, CommandSyntaxException> around;
    private @Nullable Range<Double> range;

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
      if (this.radius == null) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, "radius");
      }
      final PosArgument center = this.center == null ? EnhancedPosArgumentType.HERE_INT : this.center;
      return source -> {
        final Vec3d absoluteCenter = center.toAbsolutePos(source);
        final Vec3d around = this.around == null ? new Vec3d(0, 1, 0) : this.around.apply(source, absoluteCenter).normalize();
        final Vec3d radius = this.radius.map(d -> {
          Vec3d crossProduct = around.crossProduct(new Vec3d(0, 1, 0));
          if (crossProduct.lengthSquared() == 0) {
            crossProduct = around.y >= 0 ? new Vec3d(1, 0, 0) : new Vec3d(-1, 0, 0);
          } else {
            crossProduct = crossProduct.multiply(1 / crossProduct.length());
          }
          return crossProduct.multiply(d);
        }, Function.identity());
        return new CircleCurve(radius, absoluteCenter, around, range == null ? FULL_TURN_RANGE : range);
      };
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      parseRadius(parser, suggestionsOnly);
      while (true) {
        final int cursorBeforeWhite = parser.reader.getCursor();
        parser.reader.skipWhitespace();
        if (parser.reader.getCursor() > cursorBeforeWhite) {
          if (parseAdditionalParameters(parser, suggestionsOnly)) {
            break;
          }
        } else {
          break;
        }
      }
    }

    /**
     * 解析半径。即：{@code <double> | from (<vector> | <double> <direction>)}。
     */
    private void parseRadius(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final int cursorBeforeKeyword = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("from", suggestionsBuilder));
      if ("from".equals(unquotedString)) {
        parser.suggestionProviders.clear();
        ParsingUtil.expectAndSkipWhitespace(reader);
        radius = Either.right(ParsingUtil.parseVec3d(parser));
      } else {
        reader.setCursor(cursorBeforeKeyword);
        if (!CommandSource.shouldSuggest(reader.getRemaining(), "from")) parser.suggestionProviders.clear();

        radius = Either.left(reader.readDouble());
      }
    }

    private boolean parseAdditionalParameters(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestionProviders.clear();
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (center == null) ParsingUtil.suggestString("at", suggestionsBuilder);
        if (around == null) CommandSource.suggestMatching(List.of("around", "facing", "rotated"), suggestionsBuilder);
        if (range == null) ParsingUtil.suggestString("ranging", suggestionsBuilder);
      });
      final int cursorBeforeKeyword = parser.reader.getCursor();
      final String unquotedString = parser.reader.readUnquotedString();
      if (unquotedString.isEmpty()) {
        return true;
      } else if ("at".equals(unquotedString)) {
        if (center != null) {
          parser.reader.setCursor(cursorBeforeKeyword);
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parser.reader, unquotedString);
        }
        parser.suggestionProviders.clear();
        ParsingUtil.expectAndSkipWhitespace(parser.reader);
        center = ParsingUtil.suggestParserFromType(EnhancedPosArgumentType.posPreferringCenteredInt(), parser, suggestionsOnly);
      } else if ("around".equals(unquotedString) || "rotated".equals(unquotedString) || "facing".equals(unquotedString)) {
        if (around != null) {
          parser.reader.setCursor(cursorBeforeKeyword);
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parser.reader, unquotedString);
        }
        parser.suggestionProviders.clear();
        ParsingUtil.expectAndSkipWhitespace(parser.reader);
        switch (unquotedString) {
          case "around" -> {
            final Vec3d vec3d = ParsingUtil.parseVec3d(parser);
            around = (source, ignored) -> vec3d;
          }
          case "rotated" -> {
            final PosArgument posArgument = ParsingUtil.suggestParserFromType(new RotationArgumentType(), parser, suggestionsOnly);
            around = (source, ignored) -> {
              final Vec2f rotation = posArgument.toAbsoluteRotation(source);
              return new Vec3d(0, 0, 1).rotateX(-MathHelper.RADIANS_PER_DEGREE * rotation.x).rotateY(-MathHelper.RADIANS_PER_DEGREE * rotation.y);
            };
          }
          case "facing" -> {
            final PosArgument posArgument = ParsingUtil.suggestParserFromType(EnhancedPosArgumentType.posPreferringCenteredInt(), parser, suggestionsOnly);
            around = (source, center) -> {
              final Vec3d facingTarget = posArgument.toAbsolutePos(source);
              return facingTarget.subtract(center).normalize();
            };
          }
        }
      } else if ("ranging".equals(unquotedString)) {
        if (range != null) {
          parser.reader.setCursor(cursorBeforeKeyword);
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parser.reader, unquotedString);
        }
        parser.suggestionProviders.clear();
        ParsingUtil.expectAndSkipWhitespace(parser.reader);
        range = parseAngleRange(parser);
      } else {
        parser.reader.setCursor(cursorBeforeKeyword);
        throw ModCommandExceptionTypes.UNKNOWN_KEYWORD.createWithContext(parser.reader, unquotedString);
      }
      return false;
    }

    private Range<Double> parseAngleRange(SuggestedParser parser) throws CommandSyntaxException {
      final double firstAngle = ParsingUtil.parseAngle(parser);
      parser.suggestionProviders.clear();
      final StringReader reader = parser.reader;
      final int cursorBeforeWhitespace = reader.getCursor();
      reader.skipWhitespace();
      if (reader.getString().startsWith("..", reader.getCursor())) {
        reader.setCursor(reader.getCursor() + "..".length());
        reader.skipWhitespace();
        final double secondAngle = ParsingUtil.parseAngle(parser);
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
