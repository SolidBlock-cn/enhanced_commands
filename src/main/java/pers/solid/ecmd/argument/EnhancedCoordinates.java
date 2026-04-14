package pers.solid.ecmd.argument;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.LocalCoordinatesAccessor;
import pers.solid.ecmd.mixins.accessor.WorldCoordinatesAccessor;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @see Coordinates
 */
public interface EnhancedCoordinates extends Coordinates, ExpressionConvertible {
  MapCodec<EnhancedCoordinates> MAP_BASED_CODEC = Type.CODEC.dispatchMap(posArgument -> {
    if (posArgument instanceof WorldCoordinates | posArgument instanceof DefaultPos) {
      return Type.DEFAULT;
    } else if (posArgument instanceof LookingPos) {
      return Type.LOOKING_POS;
    } else {
      return Type.UNKNOWN;
    }
  }, type ->
      switch (type) {
        case DEFAULT -> DefaultPos.CODEC;
        case LOOKING_POS -> LookingPos.CODEC;
        default -> UnknownPos.ALWAYS_FAIL;
      });
  Codec<DefaultPos> LIST_BASED_CODEC = Codec.DOUBLE.listOf(3, 3).xmap(doubles -> DefaultPos.doubleBased(doubles.get(0), doubles.get(1), doubles.get(2), false, false, false), defaultPos -> List.of(defaultPos.x, defaultPos.y, defaultPos.z));
  Codec<EnhancedCoordinates> CODEC = Codec.either(MAP_BASED_CODEC.codec(), LIST_BASED_CODEC).xmap(fsEither -> fsEither.map(Function.identity(), Function.identity()), Either::left);

  static boolean isInt(Coordinates posArgument) {
    return posArgument instanceof EnhancedCoordinates enhancedCoordinates && enhancedCoordinates.isInt();
  }

  static String asString(Coordinates posArgument) {
    if (posArgument instanceof WorldCoordinates defaultPosArgument) {
      final WorldCoordinatesAccessor a = (WorldCoordinatesAccessor) defaultPosArgument;
      final WorldCoordinate x = a.getX();
      final WorldCoordinate y = a.getY();
      final WorldCoordinate z = a.getZ();

      final StringBuilder sb = new StringBuilder();
      if (x.isRelative()) sb.append('~');
      sb.append(StringUtil.nf.format(x.get(0))).append(' ');
      if (y.isRelative()) sb.append('~');
      sb.append(StringUtil.nf.format(y.get(0))).append(' ');
      if (z.isRelative()) sb.append('~');
      sb.append(StringUtil.nf.format(z.get(0)));
      return sb.toString();
    } else if (posArgument instanceof LocalCoordinates lookingPosArgument) {
      final LocalCoordinatesAccessor a = (LocalCoordinatesAccessor) lookingPosArgument;
      return "^" + StringUtil.nf.format(a.getLeft()) + " ^" + StringUtil.nf.format(a.getUp()) + " ^" + StringUtil.nf.format(a.getForwards());
    } else if (posArgument instanceof EnhancedCoordinates enhancedCoordinates) {
      return enhancedCoordinates.asString();
    } else {
      return "<unsupported>";
    }
  }

  /**
   * @return Whether the returned position refers to integer of double.
   */
  boolean isInt();

  Vec3 toAbsolutePos(PositionProvider positionProvider);

  @Override
  default Vec3 getPosition(CommandSourceStack source) {
    return toAbsolutePos(source);
  }

  default BlockPos toAbsoluteBlockPos(PositionProvider positionProvider) {
    return BlockPos.containing(toAbsolutePos(positionProvider));
  }

  Vec2 toAbsoluteRotation(PositionProvider positionProvider);

  @Override
  default Vec2 getRotation(CommandSourceStack source) {
    return toAbsoluteRotation(source);
  }

  enum Type implements StringRepresentable {
    DEFAULT("default"),
    LOOKING_POS("looking_pos"),
    UNKNOWN("unknown");

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String getSerializedName() {
      return this.name;
    }
  }

  abstract class UnknownPos implements EnhancedCoordinates {

    private static final MapCodec<EnhancedCoordinates> ALWAYS_FAIL = Codec.EMPTY.flatXmap(unit -> DataResult.error(() -> "This type of pos argument is not supported"), posArgument -> DataResult.error(() -> "This type of pos argument is not supported"));

    protected final boolean xRelative, yRelative, zRelative;

    protected UnknownPos(boolean xRelative, boolean yRelative, boolean zRelative) {
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

  record DefaultPos(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative, @Nullable EnhancedPosArgument.IntAlignType intAlignType) implements EnhancedCoordinates {
    public static final MapCodec<DefaultPos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(DefaultPos::x),
        Codec.DOUBLE.fieldOf("y").forGetter(DefaultPos::y),
        Codec.DOUBLE.fieldOf("z").forGetter(DefaultPos::z),
        Codec.BOOL.optionalFieldOf("x_relative", false).forGetter(DefaultPos::xRelative),
        Codec.BOOL.optionalFieldOf("y_relative", false).forGetter(DefaultPos::yRelative),
        Codec.BOOL.optionalFieldOf("z_relative", false).forGetter(DefaultPos::zRelative),
        EnhancedPosArgument.IntAlignType.CODEC.optionalFieldOf("int_align_type").forGetter(defaultPos -> Optional.ofNullable(defaultPos.intAlignType()))
    ).apply(i, DefaultPos::new));

    private DefaultPos(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative, Optional<EnhancedPosArgument.IntAlignType> intAlignType) {
      this(x, y, z, xRelative, yRelative, zRelative, intAlignType.orElse(null));
    }

    public static DefaultPos doubleBased(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative) {
      return new DefaultPos(x, y, z, xRelative, yRelative, zRelative, (EnhancedPosArgument.IntAlignType) null);
    }

    public static DefaultPos intBased(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative, EnhancedPosArgument.IntAlignType intAlignType) {
      return new DefaultPos(x, y, z, xRelative, yRelative, zRelative, intAlignType);
    }

    @Override
    public Vec3 toAbsolutePos(PositionProvider positionProvider) {
      if (!xRelative && !yRelative && !zRelative) {
        return new Vec3(x, y, z);
      }
      final Vec3 position = positionProvider.getPosition$ec();
      final Vec3 vec3d = new Vec3(xRelative ? position.x + x : x, yRelative ? position.y + y : y, zRelative ? position.z + z : z);
      if (intAlignType != null) {
        final BlockPos blockPos = BlockPos.containing(vec3d);
        return intAlignType.mayAdjustToCenter(blockPos);
      }
      return vec3d;
    }

    @Override
    public Vec2 toAbsoluteRotation(PositionProvider positionProvider) {
      if (!xRelative && !yRelative) {
        return new Vec2((float) x, (float) y);
      }
      final Vec2 rotation = positionProvider.getRotation$ec();
      return new Vec2((float) (xRelative ? rotation.x + x : x), (float) (yRelative ? rotation.y + y : y));
    }

    @Override
    public BlockPos toAbsoluteBlockPos(PositionProvider positionProvider) {
      if (!xRelative && !yRelative && !zRelative) {
        return BlockPos.containing(x, y, z);
      }
      final Vec3 position = positionProvider.getPosition$ec();
      return BlockPos.containing(xRelative ? Mth.floor(position.x() + x) : x, yRelative ? Mth.floor(position.y() + y) : y, zRelative ? Mth.floor(position.z() + z) : z);
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
      return intAlignType != null;
    }

    @Override
    public String asString() {
      final StringBuilder sb = new StringBuilder();
      final boolean isInt = intAlignType != null;
      if (xRelative) sb.append('~');
      if (!xRelative || x != 0) sb.append(isInt && !xRelative ? Integer.toString((int) x) : StringUtil.nf.format(x));
      sb.append(' ');
      if (yRelative) sb.append('~');
      if (!yRelative || y != 0) sb.append(isInt && !yRelative ? Integer.toString((int) y) : StringUtil.nf.format(y));
      sb.append(' ');
      if (zRelative) sb.append('~');
      if (!zRelative || z != 0) sb.append(isInt && !zRelative ? Integer.toString((int) z) : StringUtil.nf.format(z));
      return sb.toString();
    }
  }

  /**
   * @see LocalCoordinates
   */
  record LookingPos(double x, double y, double z) implements EnhancedCoordinates {
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
    public Vec3 toAbsolutePos(PositionProvider positionProvider) {
      Vec2 vec2f = positionProvider.getRotation$ec();
      Vec3 vec3d = positionProvider.getPositionAt$ec(positionProvider);
      float f = Mth.cos((vec2f.y + 90.0F) * ((float) Math.PI / 180F));
      float g = Mth.sin((vec2f.y + 90.0F) * ((float) Math.PI / 180F));
      float h = Mth.cos(-vec2f.x * ((float) Math.PI / 180F));
      float i = Mth.sin(-vec2f.x * ((float) Math.PI / 180F));
      float j = Mth.cos((-vec2f.x + 90.0F) * ((float) Math.PI / 180F));
      float k = Mth.sin((-vec2f.x + 90.0F) * ((float) Math.PI / 180F));
      Vec3 vec3d2 = new Vec3(f * h, i, g * h);
      Vec3 vec3d3 = new Vec3(f * j, k, g * j);
      Vec3 vec3d4 = vec3d2.cross(vec3d3).scale(-1.0F);
      double d = vec3d2.x * this.z + vec3d3.x * this.y + vec3d4.x * this.x;
      double e = vec3d2.y * this.z + vec3d3.y * this.y + vec3d4.y * this.x;
      double l = vec3d2.z * this.z + vec3d3.z * this.y + vec3d4.z * this.x;
      return new Vec3(vec3d.x + d, vec3d.y + e, vec3d.z + l);
    }

    @Override
    public Vec2 toAbsoluteRotation(PositionProvider positionProvider) {
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
    public String asString() {
      final StringBuilder sb = new StringBuilder();
      sb.append('^');
      if (x() != 0) sb.append(StringUtil.nf.format(x()));
      sb.append(" ^");
      if (y() != 0) sb.append(StringUtil.nf.format(y()));
      sb.append(" ^");
      if (z() != 0) sb.append(StringUtil.nf.format(z()));
      return sb.toString();
    }
  }
}
