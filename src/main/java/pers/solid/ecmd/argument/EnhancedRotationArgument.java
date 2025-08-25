package pers.solid.ecmd.argument;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;

public record EnhancedRotationArgument(float x, float y, boolean xRelative, boolean yRelative) implements PosArgument, ExpressionConvertible {
  public static final Codec<EnhancedRotationArgument> CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.FLOAT.fieldOf("x").forGetter(EnhancedRotationArgument::x),
      Codec.FLOAT.fieldOf("y").forGetter(EnhancedRotationArgument::y),
      Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(EnhancedRotationArgument::xRelative),
      Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(EnhancedRotationArgument::yRelative)
  ).apply(i, EnhancedRotationArgument::new));

  public Vec2f toAbsoluteRotation(PositionProvider positionProvider) {
    final Vec2f rotation$ec = positionProvider.getRotation$ec();
    return new Vec2f(xRelative ? rotation$ec.x + x : x, yRelative ? rotation$ec.y : y);
  }

  @Override
  public Vec3d getPos(ServerCommandSource source) {
    return source.getPosition();
  }

  @Override
  public Vec2f getRotation(ServerCommandSource source) {
    return this.toAbsoluteRotation((PositionProvider) source);
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
  public @NotNull String asString() {
    final StringBuilder sb = new StringBuilder();
    if (xRelative) sb.append('~');
    if (!xRelative || x != 0) sb.append(x);
    sb.append(' ');
    if (yRelative) sb.append('~');
    if (!yRelative || y != 0) sb.append(y);
    return sb.toString();
  }
}
