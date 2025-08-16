package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public record SphereRegion(double radius, Vec3d center) implements Region {
  public static final SimpleCommandExceptionType EXPAND_FAILED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.region.exception.sphere_cannot_expand"));
  public static final MapCodec<SphereRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("radius").forGetter(SphereRegion::radius), Vec3d.CODEC.fieldOf("center").forGetter(SphereRegion::center)).apply(i, SphereRegion::new));

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return vec3d.isInRange(center, radius);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Streams.stream(new PreciseCuboidRegion(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))).filter(blockPos -> blockPos.isWithinDistance(center, radius)).iterator();
  }

  @Override
  public @NotNull SphereRegion transformed(Function<Vec3d, Vec3d> transformation) {
    return new SphereRegion(radius, transformation.apply(center));
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.SPHERE;
  }

  @Override
  public @NotNull SphereRegion expanded(double offset) {
    return new SphereRegion(radius + offset, center);
  }

  @Override
  public @NotNull SphereRegion expanded(double offset, Direction.Type type) {
    throw new UnsupportedOperationException(EXPAND_FAILED.create());
  }

  @Override
  public @NotNull SphereRegion expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException(EXPAND_FAILED.create());
  }

  @Override
  public @NotNull SphereRegion expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException(EXPAND_FAILED.create());
  }

  @Override
  public double volume() {
    return 4d / 3d * Math.PI * Math.pow(radius, 3);
  }

  @Override
  public @NotNull String asString() {
    return "sphere(%s, %s %s %s)".formatted(radius, center.x, center.y, center.z);
  }

  @Override
  public @NotNull Box minContainingBox() {
    return Box.of(center, 2 * radius, 2 * radius, 2 * radius);
  }

  public enum Type implements RegionType<SphereRegion> {
    SPHERE_TYPE;

    @Override
    public String functionName() {
      return "sphere";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.sphere");
    }

    @Override
    public Parser parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<SphereRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<SphereRegionArgument> getArgumentCodec() {
      return SphereRegionArgument.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.MixedParams<SphereRegionArgument> {
    private @Nullable EnhancedPosArgument centerPos = null;
    private @Nullable Double radius = null;

    @Override
    public SphereRegionArgument getParseResult(ParseContext<?> parseContext) {
      return new SphereRegionArgument(radius == null ? 0 : radius, centerPos == null ? EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER : centerPos);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        if (radius != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "radius");
        }
        radius = reader.readDouble();
      } else if (paramIndex == 1) {
        if (centerPos != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "center");
        }
        final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
        centerPos = parseContext.parseAndSuggestArgument(type);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return radius == null ? 1 : 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }

    private static final Set<String> SUPPORTED_PARAMS = Set.of("radius", "center");

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return switch (paramName) {
        case "radius" -> radius != null;
        case "center" -> centerPos != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      switch (paramName) {
        case "radius" -> parseSequentialParameter(parseContext, 0);
        case "center" -> parseSequentialParameter(parseContext, 1);
      }
    }
  }
}
