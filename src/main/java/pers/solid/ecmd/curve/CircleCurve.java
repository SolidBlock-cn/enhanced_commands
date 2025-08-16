package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.Vec3dArgument;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.StringUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
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
 * <p>其中，{@code <radiusVector>} 和 {@code <axisVector>} 的解析方式为 {@code <x> <y> <z>} 或 {@code [length] <direction>}。
 * <p>当半径指定为标量时，其方向为轴向量与 y 轴正方向的向量积的方向，若轴向量为 y 轴正方向或负方向，则其方向为 x 轴正方向或负方向。
 *
 * @param radius   圆的半径，是一个相对向量。
 * @param center   圆的中心。
 * @param axis     旋转轴，通常应该要和 {@code radius} 垂直。
 * @param minAngle 初始旋转角。
 * @param maxAngle 终止旋转角。
 */
public record CircleCurve(Vec3d radius, Vec3d center, Vec3d axis, double minAngle, double maxAngle) implements Curve {
  public static final double FULL_MIN = 0;
  public static final double FULL_MAX = 2d * Math.PI;

  @Override
  public @NotNull Iterator<Vec3d> iteratePoints(Number interval) {
    return new AbstractIterator<>() {
      final double c = 2 * Math.PI * radius.length();
      private final Vector3d radiusVec = new Vector3d(radius.x, radius.y, radius.z);
      private final AxisAngle4d axisAngle4d = new AxisAngle4d(minAngle, axis.x, axis.y, axis.z);

      @Override
      protected Vec3d computeNext() {
        if (axisAngle4d.angle > maxAngle) {
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
    return radius.length() * (maxAngle - minAngle);
  }

  @Override
  public @NotNull CircleCurve transformed(Function<Vec3d, Vec3d> transformation) {
    return new CircleCurve(radius, transformation.apply(center), axis, minAngle, maxAngle);
  }

  @Override
  public @NotNull CircleCurve moved(@NotNull Vec3d relativePos) {
    return new CircleCurve(radius, center.add(relativePos), axis, minAngle, maxAngle);
  }

  @Override
  public @NotNull CircleCurve rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return new CircleCurve(GeoUtil.rotate(radius, blockRotation, Vec3d.ZERO), GeoUtil.rotate(center, blockRotation, pivot), GeoUtil.rotate(axis, blockRotation, Vec3d.ZERO), minAngle, maxAngle);
  }

  @Override
  public @NotNull CircleCurve mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    return new CircleCurve(GeoUtil.mirror(radius, axis, Vec3d.ZERO), GeoUtil.mirror(center, axis, pivot), GeoUtil.mirror(this.axis, axis, Vec3d.ZERO), minAngle, maxAngle);
  }

  @Override
  public @NotNull String asString() {
    if (axis.subtract(0, 1, 0).equals(Vec3d.ZERO)) {
      // axis 是 y 轴上的单位向量
      if (radius.subtract(1, 0, 0).equals(Vec3d.ZERO)) {
        if (minAngle == FULL_MIN && maxAngle == FULL_MAX) {
          // 表示一个最简单的旋转，绕 y 轴正方向，从 x 正方向开始旋转一周
          return "circle(%s at %s)".formatted(radius.y, StringUtil.wrapVector(center));
        } else {
          // 表示绕 y 轴正方向，从 x 正方向开始旋转一个特定的范围
          return "circle(%s at %s ranging %s)".formatted(radius.y, StringUtil.wrapVector(center), wrapRadRange(minAngle, maxAngle));
        }
      }
    }
    if (minAngle == FULL_MIN && maxAngle == FULL_MAX) {
      // 这种情况下，由于本来就是旋转一整周，因此没有必要指定开始坐标。
      if (radius.dotProduct(axis) == 0) {
        // 半径向量和轴向量垂直。
        return "circle(%s at %s around %s)".formatted(radius.length(), StringUtil.wrapVector(center), wrapVector(axis));
      } else {
        return "circle(from %s at %s around %s)".formatted(wrapRadius(radius), StringUtil.wrapVector(center), wrapVector(axis));
      }
    } else {
      return "circle(from %s at %s around %s ranging %s)".formatted(wrapRadius(radius), StringUtil.wrapVector(center), wrapVector(axis), wrapRadRange(minAngle, maxAngle));
    }
  }

  @Override
  public @Nullable Box minContainingBox() {
    double minX, minY, minZ, maxX, maxY, maxZ;
    minX = minY = minZ = maxX = maxY = maxZ = 0;
    final Iterator<Vec3d> vec3dIterator = iteratePoints(1);
    if (!vec3dIterator.hasNext()) {
      // 含有零个点时，返回空。
      return null;
    }
    while (vec3dIterator.hasNext()) {
      final Vec3d next = vec3dIterator.next();
      minX = Math.min(minX, next.x);
      minY = Math.min(minY, next.y);
      minZ = Math.min(minZ, next.z);
      maxX = Math.max(maxX, next.x);
      maxY = Math.max(maxY, next.y);
      maxZ = Math.max(maxZ, next.z);
    }

    return new Box(minX, minY, minZ, maxX, maxY, maxZ);
  }

  @Override
  public @NotNull Type getType() {
    return CurveTypes.CIRCLE;
  }

  public static String wrapRadRange(double minAngle, double maxAngle) {
    if (minAngle == 0) {
      return maxAngle + "rad";
    } else {
      return minAngle + "rad.." + minAngle + "rad";
    }
  }

  public static String wrapVector(Vec3d vec3d) {
    return StringUtil.wrapVector(vec3d);
  }

  public static String wrapRadius(Vec3d vec3d) {
    return StringUtil.wrapVector(vec3d);
  }


  public static final MapCodec<CircleCurve> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3d.CODEC.fieldOf("radius").forGetter(CircleCurve::radius),
      Vec3d.CODEC.fieldOf("center").forGetter(CircleCurve::center),
      Vec3d.CODEC.fieldOf("axis").forGetter(CircleCurve::axis),
      Codec.DOUBLE.optionalFieldOf("min_angle", FULL_MIN).forGetter(CircleCurve::minAngle),
      Codec.DOUBLE.optionalFieldOf("max_angle", FULL_MAX).forGetter(CircleCurve::maxAngle)
  ).apply(i, CircleCurve::new));

  public enum Type implements CurveType<CircleCurve> {
    INSTANCE;

    @Override
    public @NotNull MapCodec<CircleCurve> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends CurveArgument<? extends CircleCurve>> getArgumentCodec() {
      return CircleCurveArgument.CODEC;
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
  protected static class Parser implements FunctionLikeParser.MixedParams<CurveArgument<CircleCurve>> {
    private @Nullable Double radius;
    private @Nullable EnhancedPosArgument center;
    private @Nullable Vec3dArgument axis;
    private @Nullable DoubleDoublePair range;

    @Override
    public CurveArgument<CircleCurve> getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      final EnhancedPosArgument center = this.center == null ? EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER : this.center;
      return new CircleCurveArgument(radius == null ? 1 : radius, center, axis == null ? new Vec3dArgument.Fixed(new Vec3d(0, 1, 0)) : axis, range == null ? FULL_MIN : range.firstDouble(), range == null ? FULL_MAX : range.secondDouble());
    }

    /**
     * 解析半径。即：{@code <double> | from (<vector> | <double> <direction>)}。
     */
    private void parseRadius(ParseContext<?> parseContext) throws CommandSyntaxException {
      radius = parseContext.reader().readDouble();
    }

    private DoubleDoublePair parseAngleRange(ParseContext<?> parseContext) throws CommandSyntaxException {
      final double firstAngle = parseContext.parseAndSuggestAngle(true);
      parseContext.clearSuggestion();
      final StringReader reader = parseContext.reader();
      final int cursorBeforeWhitespace = reader.getCursor();
      reader.skipWhitespace();
      if (reader.getString().startsWith("..", reader.getCursor())) {
        reader.setCursor(reader.getCursor() + "..".length());
        reader.skipWhitespace();
        final double secondAngle = parseContext.parseAndSuggestAngle(true);
        parseContext.clearSuggestion();
        return DoubleDoublePair.of(firstAngle, secondAngle);
      } else {
        reader.setCursor(cursorBeforeWhitespace);
        return DoubleDoublePair.of(0d, firstAngle);
      }
    }

    private static final Set<String> SUPPORTED_PARAMS = Set.of("radius", "center", "pivot", "range");

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return switch (paramName) {
        case "radius" -> radius != null;
        case "center" -> center != null;
        case "pivot" -> axis != null;
        case "range" -> range != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      switch (paramName) {
        case "radius" -> parseRadius(parseContext);
        case "center" -> {
          EnhancedPosArgumentType argumentType = EnhancedPosArgumentType.posPreferringCenteredInt();
          center = parseContext.parseAndSuggestArgument(argumentType);
        }
        case "pivot" -> axis = Vec3dArgument.parse(parseContext);
        case "range" -> {
          ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
          range = parseAngleRange(parseContext);
        }
      }
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        if (radius != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parseContext.reader(), "radius");
        }
        parseRadius(parseContext);
      } else if (paramIndex == 1) {
        if (center != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parseContext.reader(), "center");
        }
        EnhancedPosArgumentType argumentType = EnhancedPosArgumentType.posPreferringCenteredInt();
        center = parseContext.parseAndSuggestArgument(argumentType);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return radius == null ? 1 : 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }
  }
}
