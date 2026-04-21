package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

public sealed interface Vec3dProvider extends ExpressionConvertible {
  Codec<Vec3dProvider> CODEC = Type.CODEC.dispatch(Vec3dProvider::getType, Type::getCodec);

  /**
   * 解析双精度浮点数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。形式为 {@code (<x> <y> <z> | [length] <direction>)}。
   */
  static <S> Vec3dProvider parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    {
      parseContext.setSuggestion((context, suggestionsBuilder) -> {
        ParsingUtil.suggestDirections(suggestionsBuilder);
        ParsingUtil.suggestString("facing", suggestionsBuilder);
        return ParsingUtil.suggestString("rotated", suggestionsBuilder).buildFuture();
      });
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();

      if ("facing".equals(unquotedString)) {
        parseContext.clearSuggestion();
        ParsingUtil.expectAndSkipWhitespace(reader);
        final EnhancedCoordinates posArgument = parseContext.parseAndSuggestArgument(new EnhancedPosArgument(EnhancedPosArgument.NumberType.DOUBLE_ONLY, EnhancedPosArgument.IntAlignType.CENTERED));
        return new Facing(posArgument);
      } else if ("rotated".equals(unquotedString)) {
        parseContext.clearSuggestion();
        ParsingUtil.expectAndSkipWhitespace(reader);
        final RotationProvider rotationArgument = parseContext.parseAndSuggestArgument(EnhancedRotationArgument.INSTANCE);
        return new Rotated(rotationArgument);
      }

      final DirectionProvider byName = DirectionProvider.CODEC.byId(unquotedString);
      if (byName != null) {
        parseContext.clearSuggestion();
        return new Directional(byName, 1);
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double x = reader.readDouble();
    parseContext.clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    {
      parseContext.setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final DirectionProvider byName = DirectionProvider.CODEC.byId(unquotedString);
      if (byName != null) {
        parseContext.clearSuggestion();
        return new Directional(byName, x);
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double y = reader.readDouble();
    parseContext.clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final double z = reader.readDouble();
    final Vec3 vec3d = new Vec3(x, y, z);
    return new Fixed(vec3d);
  }

  Vec3 toActualVector(PositionProvider positionProvider);

  Type getType();

  record Fixed(Vec3 vec3d) implements Vec3dProvider {
    public static final MapCodec<Fixed> CODEC = Vec3.CODEC.fieldOf("value").xmap(Fixed::new, Fixed::vec3d);

    @Override
    public Vec3 toActualVector(PositionProvider positionProvider) {
      return vec3d;
    }

    @Override
    public Type getType() {
      return Type.FIXED;
    }

    @Override
    public String expressAsString() {
      return StringUtil.wrapVector(vec3d);
    }
  }

  record Directional(DirectionProvider directionProvider, double length) implements Vec3dProvider {
    public static final MapCodec<Directional> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DirectionProvider.CODEC.fieldOf("direction").forGetter(Directional::directionProvider),
        Codec.DOUBLE.fieldOf("length").forGetter(Directional::length)
    ).apply(i, Directional::new));

    @Override
    public Vec3 toActualVector(PositionProvider positionProvider) {
      return Vec3.atLowerCornerOf(directionProvider.apply(positionProvider).getUnitVec3i()).scale(length);
    }

    @Override
    public Type getType() {
      return Type.DIRECTIONAL;
    }

    @Override
    public String expressAsString() {
      return length() + " " + directionProvider().getSerializedName();
    }
  }

  record Rotated(RotationProvider rotation) implements Vec3dProvider {
    public static final MapCodec<Rotated> CODEC = RotationProvider.CODEC.fieldOf("rotation").xmap(Rotated::new, Rotated::rotation);

    @Override
    public Vec3 toActualVector(PositionProvider positionProvider) {
      final Vec2 r = rotation.toAbsoluteRotation(positionProvider);
      return new Vec3(0, 0, 1).xRot(-Mth.DEG_TO_RAD * r.x).yRot(-Mth.DEG_TO_RAD * r.y);
    }

    @Override
    public Type getType() {
      return Type.ROTATED;
    }

    @Override
    public String expressAsString() {
      return "rotated " + rotation.expressAsString();
    }
  }

  record Facing(EnhancedCoordinates pos) implements Vec3dProvider {
    public static final MapCodec<Facing> CODEC = EnhancedCoordinates.CODEC.fieldOf("pos").xmap(Facing::new, Facing::pos);

    @Override
    public Vec3 toActualVector(PositionProvider positionProvider) {
      final Vec3 facingTarget = pos.toAbsolutePos(positionProvider);
      return facingTarget.subtract(positionProvider.getPosition$ec()).normalize();
    }

    @Override
    public Type getType() {
      return Type.FACING;
    }

    @Override
    public String expressAsString() {
      return "facing " + pos.expressAsString();
    }
  }

  enum Type implements StringRepresentable {
    FIXED("fixed", Fixed.CODEC),
    DIRECTIONAL("directional", Directional.CODEC),
    ROTATED("rotated", Rotated.CODEC),
    FACING("facing", Facing.CODEC);
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String name;
    private final MapCodec<? extends Vec3dProvider> codec;

    Type(String name, MapCodec<? extends Vec3dProvider> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    public MapCodec<? extends Vec3dProvider> getCodec() {
      return codec;
    }
  }
}
