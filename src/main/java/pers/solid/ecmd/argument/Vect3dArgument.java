package pers.solid.ecmd.argument;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;

public sealed interface Vect3dArgument extends ExpressionConvertible {
  Codec<Vect3dArgument> CODEC = Codec.BOOL.dispatch("directional", vect3dArgument -> vect3dArgument instanceof Directional, x -> x ? Directional.CODEC : Fixed.CODEC);

  Vec3d toActualVector(PositionProvider positionProvider);

  record Fixed(Vec3d vec3d) implements Vect3dArgument {
    public static final MapCodec<Fixed> CODEC = Vec3d.CODEC.fieldOf("value").xmap(Fixed::new, Fixed::vec3d);

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      return vec3d;
    }

    @Override
    public @NotNull String asString() {
      return StringUtil.wrapVector(vec3d);
    }
  }

  record Directional(DirectionArgument directionArgument, double length) implements Vect3dArgument {
    public static final MapCodec<Directional> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DirectionArgument.CODEC.fieldOf("direction").forGetter(Directional::directionArgument),
        Codec.DOUBLE.fieldOf("length").forGetter(Directional::length)
    ).apply(i, Directional::new));

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      return Vec3d.of(directionArgument.apply(positionProvider).getVector()).multiply(length);
    }

    @Override
    public @NotNull String asString() {
      return length() + " " + directionArgument().asString();
    }
  }

  record Rotated(EnhancedPosArgument posArgument) implements Vect3dArgument {
    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      final Vec2f rotation = posArgument.toAbsoluteRotation(positionProvider);
      return new Vec3d(0, 0, 1).rotateX(-MathHelper.RADIANS_PER_DEGREE * rotation.x).rotateY(-MathHelper.RADIANS_PER_DEGREE * rotation.y);
    }

    @Override
    public @NotNull String asString() {
      return "rotated " + posArgument.asString();
    }
  }

  record Facing(EnhancedPosArgument posArgument) implements Vect3dArgument {

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      final Vec3d facingTarget = posArgument.toAbsolutePos(positionProvider);
      return facingTarget.subtract(positionProvider.getPosition$ec()).normalize();
    }

    @Override
    public @NotNull String asString() {
      return "facing " + posArgument.asString();
    }
  }
}
