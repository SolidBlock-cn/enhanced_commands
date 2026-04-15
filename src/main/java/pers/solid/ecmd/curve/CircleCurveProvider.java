package pers.solid.ecmd.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.Vec3dProvider;
import pers.solid.ecmd.util.PositionProvider;

public record CircleCurveProvider(double radius, EnhancedCoordinates center, Vec3dProvider pivot, double minAngle, double maxAngle) implements CurveProvider<CircleCurve> {
  public static final MapCodec<CircleCurveProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.DOUBLE.fieldOf("radius").forGetter(CircleCurveProvider::radius),
      EnhancedCoordinates.CODEC.fieldOf("center").forGetter(CircleCurveProvider::center),
      Vec3dProvider.CODEC.fieldOf("pivot").forGetter(CircleCurveProvider::pivot),
      Codec.DOUBLE.fieldOf("min_angle").forGetter(CircleCurveProvider::minAngle),
      Codec.DOUBLE.fieldOf("max_angle").forGetter(CircleCurveProvider::maxAngle)
  ).apply(i, CircleCurveProvider::new));

  @Override
  public CircleCurve toAbsoluteRegion(PositionProvider positionProvider) {
    final Vec3 absoluteCenter = center.toAbsolutePos(positionProvider);
    final Vec3 axis = this.pivot == null ? new Vec3(0, 1, 0) : this.pivot.toActualVector(positionProvider).normalize();
    return new CircleCurve(this.radius, absoluteCenter, axis, minAngle, maxAngle);
  }

  @Override
  public CurveType<CircleCurve> getType() {
    return CurveTypes.CIRCLE;
  }
}
