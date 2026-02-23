package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

public sealed interface Vec3iArgument extends ExpressionConvertible {
  Codec<Vec3iArgument> CODEC = Vec3iArgument.Type.CODEC.dispatch(Vec3iArgument::getType, Vec3iArgument.Type::getCodec);

  /**
   * 解析整数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。
   */
  static <S> Vec3iArgument parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    {
      parseContext.setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        parseContext.clearSuggestion();
        return new Directional(byName, 1);
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final int x = reader.readInt();
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
    final int y = reader.readInt();
    parseContext.clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final int z = reader.readInt();
    final Vec3i vec3i = new Vec3i(x, y, z);
    return new Fixed(vec3i);
  }

  Vec3i toActualVector(PositionProvider positionProvider);

  Type getType();

  record Fixed(Vec3i value) implements Vec3iArgument {
    public static final MapCodec<Fixed> CODEC = Vec3i.CODEC.fieldOf("value").xmap(Fixed::new, Fixed::value);

    @Override
    public Vec3i toActualVector(PositionProvider positionProvider) {
      return value;
    }

    @Override
    public Type getType() {
      return Type.FIXED;
    }

    @Override
    public @NotNull String asString() {
      return StringUtil.wrapVector(value);
    }
  }

  record Directional(DirectionArgument direction, int length) implements Vec3iArgument {
    public static final MapCodec<Directional> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DirectionArgument.CODEC.fieldOf("direction").forGetter(Directional::direction),
        Codec.INT.fieldOf("length").forGetter(Directional::length)
    ).apply(i, Directional::new));

    @Override
    public Vec3i toActualVector(PositionProvider positionProvider) {
      return direction.apply(positionProvider).getUnitVec3i().multiply(length);
    }

    @Override
    public Type getType() {
      return Type.DIRECTIONAL;
    }

    @Override
    public @NotNull String asString() {
      return length + " " + direction.getSerializedName();
    }
  }

  enum Type implements StringRepresentable {
    FIXED("fixed", Fixed.CODEC), DIRECTIONAL("directional", Directional.CODEC);
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String name;
    private final MapCodec<? extends Vec3iArgument> codec;

    Type(String name, MapCodec<? extends Vec3iArgument> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    public MapCodec<? extends Vec3iArgument> getCodec() {
      return codec;
    }
  }
}
