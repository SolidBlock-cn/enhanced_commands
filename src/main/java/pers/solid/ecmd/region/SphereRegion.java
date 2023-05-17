package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.block.FunctionLikeParser;

import java.util.Iterator;

public record SphereRegion(Vec3d center, double radius) implements Region {
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
    return new SphereRegion(center.add(relativePos), radius);
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
    return 4d/3d * Math.PI * Math.pow(radius, 3);
  }

  @Override
  public String asString() {
    return "sphere(%s %s %s, %s)".formatted(center.x, center.y, center.z, radius);
  }

  public enum Type implements RegionType<SphereRegion> {
    INSTANCE;

    @Override
    public @Nullable RegionArgument<SphereRegion> parse(SuggestedParser parser) throws CommandSyntaxException {
      return new Parser().parse(parser);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<SphereRegion>> {
    private PosArgument centerPos;
    private double radius;

    @Override
    public @NotNull String functionName() {
      return "sphere";
    }

    @Override
    public Text tooltip() {
      return null; // TODO: 2023/5/13, 013 tooltip
    }

    @Override
    public RegionArgument<SphereRegion> getParseResult() {
      return source -> new SphereRegion(centerPos.toAbsolutePos(source), radius);
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        parser.suggestions.add((builder, context) -> Vec3ArgumentType.vec3().listSuggestions(context, builder));
        centerPos = Vec3ArgumentType.vec3().parse(parser.reader);
      } else if (paramIndex == 1) {
        radius = parser.reader.readDouble();
      }
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }
  }
}
