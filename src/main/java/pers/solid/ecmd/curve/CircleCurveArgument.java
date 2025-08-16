package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.Vec3dArgument;
import pers.solid.ecmd.util.PositionProvider;

public record CircleCurveArgument(double radius, EnhancedPosArgument center, Vec3dArgument axis, double minAngle, double maxAngle) implements CurveArgument<CircleCurve> {
  public static final MapCodec<CircleCurveArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.DOUBLE.fieldOf("radius").forGetter(CircleCurveArgument::radius),
      EnhancedPosArgument.CODEC.fieldOf("center").forGetter(CircleCurveArgument::center),
      Vec3dArgument.CODEC.fieldOf("axis").forGetter(CircleCurveArgument::axis),
      Codec.DOUBLE.fieldOf("min_angle").forGetter(CircleCurveArgument::minAngle),
      Codec.DOUBLE.fieldOf("max_angle").forGetter(CircleCurveArgument::maxAngle)
  ).apply(i, CircleCurveArgument::new));

  @Override
  public CircleCurve toAbsoluteRegion(PositionProvider positionProvider) throws CommandSyntaxException {
    final Vec3d absoluteCenter = center.toAbsolutePos(positionProvider);
    final Vec3d axis = this.axis == null ? new Vec3d(0, 1, 0) : this.axis.toActualVector(positionProvider).normalize();
    Vec3d crossProduct = axis.crossProduct(new Vec3d(0, 1, 0));
    if (crossProduct.lengthSquared() == 0) {
      crossProduct = axis.y >= 0 ? new Vec3d(1, 0, 0) : new Vec3d(-1, 0, 0);
    } else {
      crossProduct = crossProduct.multiply(1 / crossProduct.length());
    }
    return new CircleCurve(crossProduct.multiply(this.radius), absoluteCenter, axis, minAngle, maxAngle);
  }

  @Override
  public @NotNull CircleCurve.Type getType() {
    return CurveTypes.CIRCLE;
  }
}
