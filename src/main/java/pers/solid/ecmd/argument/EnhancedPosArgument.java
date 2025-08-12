package pers.solid.ecmd.argument;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.DefaultPosArgument;
import net.minecraft.command.argument.LookingPosArgument;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.accessor.DefaultPosArgumentAccessor;
import pers.solid.ecmd.mixins.accessor.LookingPosArgumentAccessor;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.List;
import java.util.function.Function;

/**
 * @see PosArgument
 */
public interface EnhancedPosArgument extends PosArgument, ExpressionConvertible {
  MapCodec<EnhancedPosArgument> MAP_BASED_CODEC = Type.CODEC.dispatchMap(posArgument -> {
    if (posArgument instanceof DefaultPosArgument | posArgument instanceof DoublePos) {
      return Type.DEFAULT_DOUBLE;
    } else if (posArgument instanceof IntPos) {
      return Type.DEFAULT_INT;
    } else if (posArgument instanceof LookingPos) {
      return Type.LOOKING_POS;
    } else {
      return Type.UNKNOWN;
    }
  }, type ->
      switch (type) {
        case DEFAULT_INT -> IntPos.CODEC;
        case DEFAULT_DOUBLE -> DoublePos.CODEC;
        case LOOKING_POS -> LookingPos.CODEC;
        case UNKNOWN -> DefaultPos.ALWAYS_FAIL;
      });
  Codec<DoublePos> LIST_BASED_CODEC = Codec.DOUBLE.listOf(3, 3).xmap(doubles -> new DoublePos(doubles.get(0), doubles.get(1), doubles.get(2), false, false, false), doublePos -> List.of(doublePos.x, doublePos.y, doublePos.z));
  Codec<EnhancedPosArgument> CODEC = Codec.either(MAP_BASED_CODEC.codec(), LIST_BASED_CODEC).xmap(fsEither -> fsEither.map(Function.identity(), Function.identity()), Either::left);

  static boolean isInt(PosArgument posArgument) {
    return posArgument instanceof EnhancedPosArgument enhancedPosArgument && enhancedPosArgument.isInt();
  }

  static DoublePos of(Vec3d vec3d) {
    return new DoublePos(vec3d.x, vec3d.y, vec3d.z, false, false, false);
  }

  static String asString(PosArgument posArgument) {
    if (posArgument instanceof DefaultPosArgument defaultPosArgument) {
      final DefaultPosArgumentAccessor a = (DefaultPosArgumentAccessor) defaultPosArgument;
      final CoordinateArgument x = a.getX();
      final CoordinateArgument y = a.getY();
      final CoordinateArgument z = a.getZ();

      final StringBuilder sb = new StringBuilder();
      if (x.isRelative()) sb.append('~');
      sb.append(x.toAbsoluteCoordinate(0)).append(' ');
      if (y.isRelative()) sb.append('~');
      sb.append(y.toAbsoluteCoordinate(0)).append(' ');
      if (z.isRelative()) sb.append('~');
      sb.append(z.toAbsoluteCoordinate(0));
      return sb.toString();
    } else if (posArgument instanceof LookingPosArgument lookingPosArgument) {
      final LookingPosArgumentAccessor a = (LookingPosArgumentAccessor) lookingPosArgument;
      return "^" + a.getX() + " ^" + a.getY() + " ^" + a.getZ();
    } else if (posArgument instanceof EnhancedPosArgument enhancedPosArgument) {
      return enhancedPosArgument.asString();
    } else {
      return "<unsupported>";
    }
  }

  /**
   * @return Whether the returned position refers to integer of double.
   */
  boolean isInt();

  Vec3d toAbsolutePos(PositionProvider positionProvider);

  @Override
  default Vec3d toAbsolutePos(ServerCommandSource source) {
    return toAbsolutePos(((PositionProvider) source));
  }

  default BlockPos toAbsoluteBlockPos(PositionProvider positionProvider) {
    return BlockPos.ofFloored(toAbsolutePos(positionProvider));
  }

  Vec2f toAbsoluteRotation(PositionProvider positionProvider);

  @Override
  default Vec2f toAbsoluteRotation(ServerCommandSource source) {
    return toAbsoluteRotation((PositionProvider) source);
  }

  enum Type implements StringIdentifiable {
    DEFAULT_INT("default_int"),
    DEFAULT_DOUBLE("default_double"),
    LOOKING_POS("looking_pos"),
    UNKNOWN("unknown");

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return this.name;
    }
  }

  abstract class DefaultPos implements EnhancedPosArgument {

    private static final MapCodec<EnhancedPosArgument> ALWAYS_FAIL = Codec.EMPTY.flatXmap(unit -> DataResult.error(() -> "This type of pos argument is not supported"), posArgument -> DataResult.error(() -> "This type of pos argument is not supported"));

    protected final boolean xRelative, yRelative, zRelative;

    protected DefaultPos(boolean xRelative, boolean yRelative, boolean zRelative) {
      this.xRelative = xRelative;
      this.yRelative = yRelative;
      this.zRelative = zRelative;
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
      return zRelative;
    }
  }

  record DoublePos(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative) implements EnhancedPosArgument {
    public static final MapCodec<DoublePos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(DoublePos::x),
        Codec.DOUBLE.fieldOf("y").forGetter(DoublePos::y),
        Codec.DOUBLE.fieldOf("z").forGetter(DoublePos::z),
        Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(DoublePos::xRelative),
        Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(DoublePos::yRelative),
        Codec.BOOL.optionalFieldOf("z_relative", false).forGetter(DoublePos::zRelative)
    ).apply(i, DoublePos::new));

    @Override
    public Vec3d toAbsolutePos(PositionProvider positionProvider) {
      if (!xRelative && !yRelative && !zRelative) {
        return new Vec3d(x, y, z);
      }
      final Vec3d position = positionProvider.getPosition$ec();
      return new Vec3d(xRelative ? position.x + x : x, yRelative ? position.y + y : y, zRelative ? position.z + z : z);
    }

    @Override
    public Vec2f toAbsoluteRotation(PositionProvider positionProvider) {
      if (!xRelative && !yRelative) {
        return new Vec2f((float) x, (float) y);
      }
      final Vec2f rotation = positionProvider.getRotation$ec();
      return new Vec2f((float) (xRelative ? rotation.x + x : x), (float) (yRelative ? rotation.y + y : y));
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
      return zRelative;
    }

    @Override
    public boolean isInt() {
      return false;
    }

    @Override
    public @NotNull String asString() {
      final StringBuilder sb = new StringBuilder();
      if (xRelative) sb.append('~');
      if (!xRelative || x != 0) sb.append(x).append(' ');
      if (yRelative) sb.append('~');
      if (!yRelative || y != 0) sb.append(y).append(' ');
      if (zRelative) sb.append('~');
      if (!zRelative || z != 0) sb.append(z);
      return sb.toString();
    }
  }

  record IntPos(int x, int y, int z, boolean xRelative, boolean yRelative, boolean zRelative, EnhancedPosArgumentType.IntAlignType alignType) implements EnhancedPosArgument {
    public static final MapCodec<IntPos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.fieldOf("x").forGetter(IntPos::x),
        Codec.INT.fieldOf("y").forGetter(IntPos::y),
        Codec.INT.fieldOf("z").forGetter(IntPos::z),
        Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(IntPos::xRelative),
        Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(IntPos::yRelative),
        Codec.BOOL.optionalFieldOf("z_relative", false).forGetter(IntPos::zRelative),
        EnhancedPosArgumentType.IntAlignType.CODEC.optionalFieldOf("align_type", EnhancedPosArgumentType.IntAlignType.HORIZONTALLY_CENTERED).forGetter(IntPos::alignType)
    ).apply(i, IntPos::new));

    @Override
    public boolean isInt() {
      return true;
    }

    @Override
    public Vec3d toAbsolutePos(PositionProvider positionProvider) {
      final BlockPos blockPos = toAbsoluteBlockPos(positionProvider);
      return alignType.mayAdjustToCenter(blockPos);
    }

    @Override
    public Vec2f toAbsoluteRotation(PositionProvider positionProvider) {
      if (!xRelative && !yRelative) {
        return new Vec2f((float) x, (float) y);
      }
      final Vec2f rotation = positionProvider.getRotation$ec();
      return new Vec2f(xRelative ? rotation.x + x : x, yRelative ? rotation.y + y : y);
    }

    @Override
    public BlockPos toAbsoluteBlockPos(PositionProvider positionProvider) {
      if (!xRelative && !yRelative && !zRelative) {
        return new BlockPos(x, y, z);
      }
      final Vec3d position = positionProvider.getPosition$ec();
      return new BlockPos(xRelative ? MathHelper.floor(position.getX() + x) : x, yRelative ? MathHelper.floor(position.getY() + y) : y, zRelative ? MathHelper.floor(position.getZ() + z) : z);
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
      return zRelative;
    }

    @Override
    public @NotNull String asString() {
      final StringBuilder sb = new StringBuilder();
      if (xRelative) sb.append('~');
      if (!xRelative || x != 0) sb.append(x).append(' ');
      if (yRelative) sb.append('~');
      if (!yRelative || y != 0) sb.append(y).append(' ');
      if (zRelative) sb.append('~');
      if (!zRelative || z != 0) sb.append(z);
      return sb.toString();
    }
  }

  /**
   * @see LookingPosArgument
   */
  record LookingPos(double x, double y, double z) implements EnhancedPosArgument {
    public static final MapCodec<LookingPos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(LookingPos::x),
        Codec.DOUBLE.fieldOf("y").forGetter(LookingPos::y),
        Codec.DOUBLE.fieldOf("z").forGetter(LookingPos::z)
    ).apply(i, LookingPos::new));

    @Override
    public boolean isInt() {
      return false;
    }

    @Override
    public Vec3d toAbsolutePos(PositionProvider positionProvider) {
      Vec2f vec2f = positionProvider.getRotation$ec();
      Vec3d vec3d = positionProvider.getPositionAt$ec(positionProvider);
      float f = MathHelper.cos((vec2f.y + 90.0F) * ((float) Math.PI / 180F));
      float g = MathHelper.sin((vec2f.y + 90.0F) * ((float) Math.PI / 180F));
      float h = MathHelper.cos(-vec2f.x * ((float) Math.PI / 180F));
      float i = MathHelper.sin(-vec2f.x * ((float) Math.PI / 180F));
      float j = MathHelper.cos((-vec2f.x + 90.0F) * ((float) Math.PI / 180F));
      float k = MathHelper.sin((-vec2f.x + 90.0F) * ((float) Math.PI / 180F));
      Vec3d vec3d2 = new Vec3d(f * h, i, g * h);
      Vec3d vec3d3 = new Vec3d(f * j, k, g * j);
      Vec3d vec3d4 = vec3d2.crossProduct(vec3d3).multiply(-1.0F);
      double d = vec3d2.x * this.z + vec3d3.x * this.y + vec3d4.x * this.x;
      double e = vec3d2.y * this.z + vec3d3.y * this.y + vec3d4.y * this.x;
      double l = vec3d2.z * this.z + vec3d3.z * this.y + vec3d4.z * this.x;
      return new Vec3d(vec3d.x + d, vec3d.y + e, vec3d.z + l);
    }

    @Override
    public Vec2f toAbsoluteRotation(PositionProvider positionProvider) {
      return null;
    }

    @Override
    public boolean isXRelative() {
      return true;
    }

    @Override
    public boolean isYRelative() {
      return true;
    }

    @Override
    public boolean isZRelative() {
      return true;
    }

    @Override
    public @NotNull String asString() {
      final StringBuilder sb = new StringBuilder();
      sb.append('^');
      if (x() != 0) sb.append(x());
      sb.append(" ^");
      if (y() != 0) sb.append(y());
      sb.append(" ^");
      if (z() != 0) sb.append(z());
      return sb.toString();
    }
  }
}
