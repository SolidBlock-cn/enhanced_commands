package pers.solid.ecmd.argument;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;

public record RotationProvider(float x, float y, boolean xRelative, boolean yRelative) implements Coordinates, ExpressionConvertible {
  public static final Codec<RotationProvider> CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.FLOAT.fieldOf("x").forGetter(RotationProvider::x),
      Codec.FLOAT.fieldOf("y").forGetter(RotationProvider::y),
      Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(RotationProvider::xRelative),
      Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(RotationProvider::yRelative)
  ).apply(i, RotationProvider::new));

  public Vec2 toAbsoluteRotation(PositionProvider positionProvider) {
    final Vec2 rotation$ec = positionProvider.getRotation$ec();
    return new Vec2(xRelative ? rotation$ec.x + x : x, yRelative ? rotation$ec.y : y);
  }

  @Override
  public Vec3 getPosition(CommandSourceStack source) {
    return source.getPosition();
  }

  @Override
  public Vec2 getRotation(CommandSourceStack source) {
    return this.toAbsoluteRotation(source);
  }

  @Override
  public boolean isXRelative() {
    return xRelative;
  }

  @Override
  public boolean isYRelative() {
    return yRelative;
  }

  @Override
  public boolean isZRelative() {
    return true;
  }

  @Override
  public String expressAsString() {
    final StringBuilder sb = new StringBuilder();
    if (xRelative) sb.append('~');
    if (!xRelative || x != 0) sb.append(x);
    sb.append(' ');
    if (yRelative) sb.append('~');
    if (!yRelative || y != 0) sb.append(y);
    return sb.toString();
  }
}
