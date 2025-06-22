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
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.List;
import java.util.function.Function;

/**
 * @see PosArgument
 */
public interface EnhancedPosArgument extends PosArgument, ExpressionConvertible {
  MapCodec<PosArgument> MAP_BASED_CODEC = Type.CODEC.dispatchMap(posArgument -> {
    if (posArgument instanceof DefaultPosArgument | posArgument instanceof DoublePos) {
      return Type.DEFAULT_DOUBLE;
    } else if (posArgument instanceof IntPos) {
      return Type.DEFAULT_INT;
    } else if (posArgument instanceof LookingPosArgument) {
      return Type.LOOKING_POS;
    } else {
      return Type.UNKNOWN;
    }
  }, type ->
      switch (type) {
        case DEFAULT_INT -> IntPos.CODEC;
        case DEFAULT_DOUBLE -> DoublePos.CODEC_VANILLA_COMPATIBLE;
        case LOOKING_POS -> DefaultPos.LOOKING_POS_CODEC;
        case UNKNOWN -> DefaultPos.ALWAYS_FAIL;
      });
  Codec<DoublePos> LIST_BASED_CODEC = Codec.DOUBLE.listOf(3, 3).xmap(doubles -> new DoublePos(doubles.get(0), doubles.get(1), doubles.get(2), false, false, false), doublePos -> List.of(doublePos.x, doublePos.y, doublePos.z));
  Codec<PosArgument> CODEC = Codec.either(MAP_BASED_CODEC.codec(), LIST_BASED_CODEC).xmap(fsEither -> fsEither.map(Function.identity(), Function.identity()), Either::left);

  static boolean isInt(PosArgument posArgument) {
    return posArgument instanceof EnhancedPosArgument enhancedPosArgument && enhancedPosArgument.isInt();
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

    private static final MapCodec<LookingPosArgument> LOOKING_POS_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(lookingPosArgument -> ((LookingPosArgumentAccessor) lookingPosArgument).getX()),
        Codec.DOUBLE.fieldOf("y").forGetter(lookingPosArgument -> ((LookingPosArgumentAccessor) lookingPosArgument).getY()),
        Codec.DOUBLE.fieldOf("z").forGetter(lookingPosArgument -> ((LookingPosArgumentAccessor) lookingPosArgument).getZ())
    ).apply(i, LookingPosArgument::new));

    private static final MapCodec<PosArgument> ALWAYS_FAIL = Codec.EMPTY.flatXmap(unit -> DataResult.error(() -> "This type of pos argument is not supported"), posArgument -> DataResult.error(() -> "This type of pos argument is not supported"));

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

  class DoublePos extends DefaultPos {
    public static final MapCodec<DoublePos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(doublePos -> doublePos.x),
        Codec.DOUBLE.fieldOf("y").forGetter(doublePos -> doublePos.y),
        Codec.DOUBLE.fieldOf("z").forGetter(doublePos -> doublePos.z),
        Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(doublePos -> doublePos.xRelative),
        Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(doublePos -> doublePos.yRelative),
        Codec.BOOL.optionalFieldOf("z_relative", false).forGetter(doublePos -> doublePos.zRelative)
    ).apply(i, DoublePos::new));
    public static final MapCodec<PosArgument> CODEC_VANILLA_COMPATIBLE = CODEC.flatXmap(DataResult::success, posArgument -> {
      if (posArgument instanceof DoublePos doublePos) {
        return DataResult.success(doublePos);
      } else if (posArgument instanceof DefaultPosArgument defaultPosArgument) {
        DefaultPosArgumentAccessor a = (DefaultPosArgumentAccessor) defaultPosArgument;
        return DataResult.success(new DoublePos(a.getX().toAbsoluteCoordinate(0), a.getY().toAbsoluteCoordinate(0), a.getZ().toAbsoluteCoordinate(0), defaultPosArgument.isXRelative(), defaultPosArgument.isYRelative(), defaultPosArgument.isZRelative()));
      } else {
        return DataResult.error(() -> "Unknown type");
      }
    });

    private final double x, y, z;

    protected DoublePos(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative) {
      super(xRelative, yRelative, zRelative);
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public Vec3d toAbsolutePos(ServerCommandSource source) {
      if (!xRelative && !yRelative && !zRelative) {
        return new Vec3d(x, y, z);
      }
      final Vec3d position = source.getPosition();
      return new Vec3d(xRelative ? position.x + x : x, yRelative ? position.y + y : y, zRelative ? position.z + z : z);
    }

    @Override
    public Vec2f toAbsoluteRotation(ServerCommandSource source) {
      if (!xRelative && !yRelative) {
        return new Vec2f((float) x, (float) y);
      }
      final Vec2f rotation = source.getRotation();
      return new Vec2f((float) (xRelative ? rotation.x + x : x), (float) (yRelative ? rotation.y + y : y));
    }

    @Override
    public boolean isInt() {
      return false;
    }

    @Override
    public @NotNull String asString() {
      final StringBuilder sb = new StringBuilder();
      if (xRelative) sb.append('~');
      sb.append(x).append(' ');
      if (yRelative) sb.append('~');
      sb.append(y).append(' ');
      if (zRelative) sb.append('~');
      sb.append(z);
      return sb.toString();
    }
  }

  class IntPos extends DefaultPos {
    public static final MapCodec<IntPos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.fieldOf("x").forGetter(intPos -> intPos.x),
        Codec.INT.fieldOf("y").forGetter(intPos -> intPos.y),
        Codec.INT.fieldOf("z").forGetter(intPos -> intPos.z),
        Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(intPos -> intPos.xRelative),
        Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(intPos -> intPos.yRelative),
        Codec.BOOL.optionalFieldOf("z_relative", false).forGetter(intPos -> intPos.zRelative),
        EnhancedPosArgumentType.IntAlignType.CODEC.optionalFieldOf("align_type", EnhancedPosArgumentType.IntAlignType.HORIZONTALLY_CENTERED).forGetter(intPos -> intPos.intAlignType)
    ).apply(i, IntPos::new));
    private final int x, y, z;
    private final EnhancedPosArgumentType.IntAlignType intAlignType;

    protected IntPos(int x, int y, int z, boolean xRelative, boolean yRelative, boolean zRelative, EnhancedPosArgumentType.IntAlignType intAlignType) {
      super(xRelative, yRelative, zRelative);
      this.x = x;
      this.y = y;
      this.z = z;
      this.intAlignType = intAlignType;
    }

    protected IntPos(int x, int y, int z, boolean xRelative, boolean yRelative, boolean zRelative) {
      this(x, y, z, xRelative, yRelative, zRelative, EnhancedPosArgumentType.IntAlignType.CENTERED);
    }

    @Override
    public boolean isInt() {
      return true;
    }

    @Override
    public Vec3d toAbsolutePos(ServerCommandSource source) {
      final BlockPos blockPos = toAbsoluteBlockPos(source);
      return intAlignType.mayAdjustToCenter(blockPos);
    }

    @Override
    public Vec2f toAbsoluteRotation(ServerCommandSource source) {
      if (!xRelative && !yRelative) {
        return new Vec2f((float) x, (float) y);
      }
      final Vec2f rotation = source.getRotation();
      return new Vec2f(xRelative ? rotation.x + x : x, yRelative ? rotation.y + y : y);
    }

    @Override
    public BlockPos toAbsoluteBlockPos(ServerCommandSource source) {
      if (!xRelative && !yRelative && !zRelative) {
        return new BlockPos(x, y, z);
      }
      final Vec3d position = source.getPosition();
      return new BlockPos(xRelative ? MathHelper.floor(position.getX() + x) : x, yRelative ? MathHelper.floor(position.getY() + y) : y, zRelative ? MathHelper.floor(position.getZ() + z) : z);
    }

    @Override
    public @NotNull String asString() {
      final StringBuilder sb = new StringBuilder();
      if (xRelative) sb.append('~');
      sb.append(x).append(' ');
      if (yRelative) sb.append('~');
      sb.append(y).append(' ');
      if (zRelative) sb.append('~');
      sb.append(z);
      return sb.toString();
    }
  }
}
