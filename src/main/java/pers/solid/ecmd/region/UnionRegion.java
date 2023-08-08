package pers.solid.ecmd.region;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record UnionRegion(Collection<Region> regions) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return regions.stream().anyMatch(region -> region.contains(vec3d));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return regions.stream().anyMatch(region -> region.contains(vec3i));
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return stream().iterator();
  }

  @Override
  public Stream<BlockPos> stream() {
    return regions.stream().flatMap(Region::stream).map(BlockPos::toImmutable).distinct();
  }

  @Override
  public @NotNull UnionRegion moved(@NotNull Vec3d relativePos) {
    return new UnionRegion(Collections2.transform(regions, region -> region.moved(relativePos)));
  }

  @Override
  public @NotNull UnionRegion moved(@NotNull Vec3i relativePos) {
    return new UnionRegion(Collections2.transform(regions, region -> region.moved(relativePos)));
  }

  @Override
  public @NotNull UnionRegion rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    return new UnionRegion(Collections2.transform(regions, region -> region.rotated(center, blockRotation)));
  }

  @Override
  public @NotNull UnionRegion mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    return new UnionRegion(Collections2.transform(regions, region -> region.mirrored(center, axis)));
  }

  @Override
  public @NotNull RegionType<UnionRegion> getType() {
    return RegionTypes.UNION;
  }

  /**
   * The value is inaccurate. The actual value equals or is lower than it.
   */
  @Override
  public double volume() {
    return regions.stream().mapToDouble(Region::volume).sum();
  }

  @Override
  public @NotNull String asString() {
    return "union(" + String.join(", ", Collections2.transform(regions, Region::asString)) + ")";
  }

  public enum Type implements RegionType<UnionRegion> {
    UNION_TYPE;

    @Override
    public @Nullable RegionArgument<?> parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<UnionRegion>> {
    private final List<RegionArgument<?>> regions = new ArrayList<>();

    @Override
    public @NotNull String functionName() {
      return "union";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.union");
    }

    @Override
    public RegionArgument<UnionRegion> getParseResult() {
      return source -> new UnionRegion(regions.stream().map(regionArgument -> (Region) regionArgument.toAbsoluteRegion(source)).toList());
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      regions.add(RegionArgument.parse(parser, suggestionsOnly));
    }
  }
}
