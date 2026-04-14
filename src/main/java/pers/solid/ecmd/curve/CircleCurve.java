package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.Vec3dProvider;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.StringUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * <p>完整的圆或者仅一个圆弧。语法规则：
 * <p>{@code circle(<radius>, <center>, pivot = <pivot>, range = <range>)} 或<br>
 * {@code circle(radius = <radius>, center = <center>, pivot = <axis>, range = <range>)}
 *
 * <p>其中，{@code pivot} 参数支持以下写法：
 * <ul>
 *   <li>{@code pivot = <向量>}：直接指定由三个数表示的向量。
 *   <li>{@code pivot = rotated <y> <x>}：表示由玩家的偏航角和俯仰角确定的方向向量。
 *   <li>{@code pivot = facing <坐标>}：朝向指定的坐标的方向向量。
 * </ul>
 *
 * @param radius   圆的半径，是一个双精度浮点数。
 * @param center   圆的中心。
 * @param pivot    旋转轴。
 * @param minAngle 初始旋转角。
 * @param maxAngle 终止旋转角。
 */
public record CircleCurve(double radius, Vec3 center, Vec3 pivot, double minAngle, double maxAngle) implements Curve {
  public static final double FULL_MIN = 0;
  public static final double FULL_MAX = 2d * Math.PI;
  public static final MapCodec<CircleCurve> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("radius").forGetter(CircleCurve::radius),
      Vec3.CODEC.fieldOf("center").forGetter(CircleCurve::center),
      Vec3.CODEC.fieldOf("pivot").forGetter(CircleCurve::pivot),
      Codec.DOUBLE.optionalFieldOf("min_angle", FULL_MIN).forGetter(CircleCurve::minAngle),
      Codec.DOUBLE.optionalFieldOf("max_angle", FULL_MAX).forGetter(CircleCurve::maxAngle)
  ).apply(i, CircleCurve::new));

  public static String wrapRadRange(double minAngle, double maxAngle) {
    if (minAngle == 0) {
      return maxAngle + "rad";
    } else {
      return minAngle + "rad.." + minAngle + "rad";
    }
  }

  public static String wrapVector(Vec3 vec3d) {
    return StringUtil.wrapVector(vec3d);
  }

  @Override
  public Iterator<Vec3> iteratePoints(Number interval) {
    final Vector3d radiusVecStart = new Vector3d(pivot.x, pivot.y, pivot.z).cross(0, 1, 0);
    if (radiusVecStart.lengthSquared() == 0) {
      if (pivot.y >= 0) {
        radiusVecStart.set(1, 0, 0);
      } else {
        radiusVecStart.set(-1, 0, 0);
      }
    } else {
      radiusVecStart.normalize();
    }
    radiusVecStart.mul(radius);
    return new AbstractIterator<>() {
      private final Vector3d radiusVec = new Vector3d(radiusVecStart);
      private final AxisAngle4d axisAngle4d = new AxisAngle4d(minAngle, pivot.x, pivot.y, pivot.z);

      @Override
      protected Vec3 computeNext() {
        if (axisAngle4d.angle > maxAngle || Double.isNaN(axisAngle4d.angle)) {
          return endOfData();
        }
        radiusVec.set(radiusVecStart);
        axisAngle4d.transform(radiusVec);
        axisAngle4d.angle += Math.abs(interval.doubleValue() / radius);
        return new Vec3(radiusVec.x + center.x, radiusVec.y + center.y, radiusVec.z + center.z);
      }
    };
  }

  @Override
  public double length() {
    return radius * (maxAngle - minAngle);
  }

  @Override
  public CircleCurve transformed(Function<Vec3, Vec3> transformation) {
    return new CircleCurve(radius, transformation.apply(center), pivot, minAngle, maxAngle);
  }

  @Override
  public CircleCurve moved(Vec3 relativePos) {
    return new CircleCurve(radius, center.add(relativePos), pivot, minAngle, maxAngle);
  }

  @Override
  public CircleCurve rotated(Rotation blockRotation, Vec3 pivot) {
    return new CircleCurve(radius, GeoUtil.rotate(center, blockRotation, pivot), GeoUtil.rotate(pivot, blockRotation, Vec3.ZERO), minAngle, maxAngle);
  }

  @Override
  public CircleCurve mirrored(Direction.Axis axis, Vec3 pivot) {
    return new CircleCurve(radius, GeoUtil.mirror(center, axis, pivot), GeoUtil.mirror(this.pivot, axis, Vec3.ZERO), minAngle, maxAngle);
  }

  @Override
  public String asString() {
    StringJoiner joiner = new StringJoiner(", ", "circle(", ")");
    joiner.add("radius = " + radius);
    joiner.add("center = " + StringUtil.wrapVector(center));
    if (pivot.x != 0 || pivot.y != 1 || pivot.z != 0) {
      joiner.add("pivot = " + StringUtil.wrapVector(pivot));
    }
    if (minAngle != FULL_MIN || maxAngle != FULL_MAX) {
      joiner.add("range = " + wrapRadRange(minAngle, maxAngle));
    }
    return joiner.toString();
  }

  @Override
  public @Nullable AABB minContainingBox() {
    double minX, minY, minZ, maxX, maxY, maxZ;
    minX = minY = minZ = Double.POSITIVE_INFINITY;
    maxX = maxY = maxZ = Double.NEGATIVE_INFINITY;
    final Iterator<Vec3> vec3dIterator = iteratePoints(radius * Mth.DEG_TO_RAD * 30); // 30 度
    if (!vec3dIterator.hasNext()) {
      // 含有零个点时，返回空。
      return null;
    }
    while (vec3dIterator.hasNext()) {
      final Vec3 next = vec3dIterator.next();
      minX = Math.min(minX, next.x);
      minY = Math.min(minY, next.y);
      minZ = Math.min(minZ, next.z);
      maxX = Math.max(maxX, next.x);
      maxY = Math.max(maxY, next.y);
      maxZ = Math.max(maxZ, next.z);
    }

    return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
  }

  @Override
  public Type getType() {
    return CurveTypes.CIRCLE;
  }

  public enum Type implements CurveType<CircleCurve> {
    INSTANCE;

    @Override
    public MapCodec<CircleCurve> getCodec() {
      return CODEC;
    }

    @Override
    public MapCodec<? extends CurveProvider<? extends CircleCurve>> getArgumentCodec() {
      return CircleCurveProvider.CODEC;
    }
  }

  /**
   * 语法：
   * <pre>{@code
   *   circle(<radius>, <center>, pivot = <pivot>, range = <range>)
   *   circle(radius = <radius>, center = <center>, pivot = <pivot>, range = <range>)
   *
   *   <pivot> = <vec> | <direction> | <length> <direction> | facing <pos> | rotated <y> <z>
   *   <range> = <angle>[..<angle>]
   *   <angle> = <double>deg|<double>turn|<double>rad|0
   * }</pre>
   * <p>
   * 其中：{@code <range>} 的默认值为 {@code 0turn..1turn}，{@code <pivot>} 的默认值为 {@code 0 1 0}。
   */
  protected static class Parser implements FunctionContentParser.MixedParams<CurveProvider<CircleCurve>> {
    private static final Set<String> SUPPORTED_PARAMS = Set.of("radius", "center", "pivot", "range");
    private @Nullable Double radius;
    private @Nullable EnhancedCoordinates center;
    private @Nullable Vec3dProvider axis;
    private @Nullable DoubleDoublePair range;

    @Override
    public CurveProvider<CircleCurve> getParseResult(ParseContext<?> parseContext) {
      final EnhancedCoordinates center = this.center == null ? EnhancedPosArgument.CURRENT_BLOCK_POS_CENTER : this.center;
      return new CircleCurveProvider(radius == null ? 1 : radius, center, axis == null ? new Vec3dProvider.Fixed(new Vec3(0, 1, 0)) : axis, range == null ? FULL_MIN : Math.min(range.firstDouble(), range.secondDouble()), range == null ? FULL_MAX : Math.max(range.firstDouble(), range.secondDouble()));
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
        case "radius" -> radius = parseContext.reader().readDouble();
        case "center" -> {
          EnhancedPosArgument argumentType = EnhancedPosArgument.posPreferringCenteredInt();
          center = parseContext.parseAndSuggestArgument(argumentType);
        }
        case "pivot" -> axis = Vec3dProvider.parse(parseContext);
        case "range" -> range = parseAngleRange(parseContext);
      }
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        if (radius != null) {
          throw EnhancedCommandsCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parseContext.reader(), "radius");
        }
        radius = parseContext.reader().readDouble();
      } else if (paramIndex == 1) {
        if (center != null) {
          throw EnhancedCommandsCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(parseContext.reader(), "center");
        }
        EnhancedPosArgument argumentType = EnhancedPosArgument.posPreferringCenteredInt();
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
