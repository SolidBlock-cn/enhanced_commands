package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.block.FunctionLikeParser;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Iterator;

public record SphereRegion(double radius, Vec3d center) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return vec3d.isInRange(center, radius);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Streams.stream(new CuboidRegion(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))).filter(blockPos -> blockPos.isWithinDistance(center, radius)).iterator();
  }

  @Override
  public @NotNull SphereRegion moved(@NotNull Vec3d relativePos) {
    return new SphereRegion(radius, center.add(relativePos));
  }

  @Override
  public @NotNull Region rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NotNull Region mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NotNull RegionType<?> getType() {
    return RegionTypes.SPHERE;
  }

  @Override
  public double volume() {
    return 4d / 3d * Math.PI * Math.pow(radius, 3);
  }

  @Override
  public String asString() {
    return "sphere(%s %s %s, %s)".formatted(center.x, center.y, center.z, radius);
  }

  public enum Type implements RegionType<SphereRegion> {
    INSTANCE;

    @Override
    public @Nullable RegionArgument<SphereRegion> parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<SphereRegion>> {
    private PosArgument centerPos = EnhancedPosArgumentType.HERE_DOUBLE;
    private double radius;

    @Override
    public @NotNull String functionName() {
      return "sphere";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.sphere");
    }

    @Override
    public RegionArgument<SphereRegion> getParseResult() {
      return source -> new SphereRegion(radius, centerPos.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false);
      if (paramIndex == 0) {
        radius = parser.reader.readDouble();
      } else if (paramIndex == 1) {
        centerPos = SuggestionUtil.suggestParserFromType(type, parser, suggestionsOnly);
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }
  }
}
