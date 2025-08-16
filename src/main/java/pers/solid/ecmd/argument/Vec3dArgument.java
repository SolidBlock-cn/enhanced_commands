package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

public sealed interface Vec3dArgument extends ExpressionConvertible {
  Codec<Vec3dArgument> CODEC = Type.CODEC.dispatch(Vec3dArgument::getType, Type::getCodec);

  /**
   * 解析双精度浮点数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。形式为 {@code (<x> <y> <z> | [length] <direction>)}。
   */
  static <S> Vec3dArgument parse(ParseContext<S> parseContext) throws CommandSyntaxException {
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
        final EnhancedPosArgument posArgument = parseContext.parseAndSuggestArgument(EnhancedPosArgumentType.posPreferringCenteredInt());
        return new Facing(posArgument);
      } else if ("rotated".equals(unquotedString)) {
        parseContext.clearSuggestion();
        ParsingUtil.expectAndSkipWhitespace(reader);
        final EnhancedRotationArgument rotationArgument = parseContext.parseAndSuggestArgument(EnhancedRotationArgumentType.INSTANCE);
        return new Rotated(rotationArgument);
      }

      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
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
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
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
    final Vec3d vec3d = new Vec3d(x, y, z);
    return new Fixed(vec3d);
  }

  Vec3d toActualVector(PositionProvider positionProvider);

  Type getType();

  record Fixed(Vec3d vec3d) implements Vec3dArgument {
    public static final MapCodec<Fixed> CODEC = Vec3d.CODEC.fieldOf("value").xmap(Fixed::new, Fixed::vec3d);

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      return vec3d;
    }

    @Override
    public Type getType() {
      return Type.FIXED;
    }

    @Override
    public @NotNull String asString() {
      return StringUtil.wrapVector(vec3d);
    }
  }

  record Directional(DirectionArgument directionArgument, double length) implements Vec3dArgument {
    public static final MapCodec<Directional> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DirectionArgument.CODEC.fieldOf("direction").forGetter(Directional::directionArgument),
        Codec.DOUBLE.fieldOf("length").forGetter(Directional::length)
    ).apply(i, Directional::new));

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      return Vec3d.of(directionArgument.apply(positionProvider).getVector()).multiply(length);
    }

    @Override
    public Type getType() {
      return Type.DIRECTIONAL;
    }

    @Override
    public @NotNull String asString() {
      return length() + " " + directionArgument().asString();
    }
  }

  record Rotated(EnhancedRotationArgument rotation) implements Vec3dArgument {
    public static final MapCodec<Rotated> CODEC = EnhancedRotationArgument.CODEC.fieldOf("rotation").xmap(Rotated::new, Rotated::rotation);

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      final Vec2f r = rotation.toAbsoluteRotation(positionProvider);
      return new Vec3d(0, 0, 1).rotateX(-MathHelper.RADIANS_PER_DEGREE * r.x).rotateY(-MathHelper.RADIANS_PER_DEGREE * r.y);
    }

    @Override
    public Type getType() {
      return Type.ROTATED;
    }

    @Override
    public @NotNull String asString() {
      return "rotated " + rotation.asString();
    }
  }

  record Facing(EnhancedPosArgument pos) implements Vec3dArgument {
    public static final MapCodec<Facing> CODEC = EnhancedPosArgument.CODEC.fieldOf("pos").xmap(Facing::new, Facing::pos);

    @Override
    public Vec3d toActualVector(PositionProvider positionProvider) {
      final Vec3d facingTarget = pos.toAbsolutePos(positionProvider);
      return facingTarget.subtract(positionProvider.getPosition$ec()).normalize();
    }

    @Override
    public Type getType() {
      return Type.FACING;
    }

    @Override
    public @NotNull String asString() {
      return "facing " + pos.asString();
    }
  }

  enum Type implements StringIdentifiable {
    FIXED("fixed", Fixed.CODEC),
    DIRECTIONAL("directional", Directional.CODEC),
    ROTATED("rotated", Rotated.CODEC),
    FACING("facing", Facing.CODEC);
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String name;
    private final MapCodec<? extends Vec3dArgument> codec;

    Type(String name, MapCodec<? extends Vec3dArgument> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String asString() {
      return name;
    }

    public MapCodec<? extends Vec3dArgument> getCodec() {
      return codec;
    }
  }
}
