package pers.solid.ecmd.region;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record IntersectRegion(Collection<Region> regions) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return regions.stream().allMatch(region -> region.contains(vec3d));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return regions.stream().allMatch(region -> region.contains(vec3i));
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return stream().iterator();
  }

  @Override
  public Stream<BlockPos> stream() {
    if (regions.isEmpty()) {
      return Stream.empty();
    }
    return regions.iterator().next().stream()
        .filter(blockPos -> regions.stream().allMatch(region -> region.contains(blockPos)));
  }

  @Override
  public @NotNull IntersectRegion moved(@NotNull Vec3d relativePos) {
    return new IntersectRegion(Collections2.transform(regions, region -> region.moved(relativePos)));
  }

  @Override
  public @NotNull IntersectRegion moved(@NotNull Vec3i relativePos) {
    return new IntersectRegion(Collections2.transform(regions, region -> region.moved(relativePos)));
  }

  @Override
  public @NotNull IntersectRegion rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return new IntersectRegion(Collections2.transform(regions, region -> region.rotated(pivot, blockRotation)));
  }

  @Override
  public @NotNull IntersectRegion mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return new IntersectRegion(Collections2.transform(regions, region -> region.mirrored(pivot, axis)));
  }

  @Override
  public @NotNull RegionType<IntersectRegion> getType() {
    return RegionTypes.INTERSECT;
  }

  /**
   * The volume of the intersect region is inaccurate. The actual value equals to of lower than it.
   */
  @Override
  public double volume() {
    return regions.stream().mapToDouble(Region::volume).min().orElse(0);
  }

  /**
   * 和 {@link #volume()} 类似，其返回值是各区域中的最小值。
   */
  @Override
  public long numberOfBlocksAffected() {
    return regions.stream().mapToLong(Region::numberOfBlocksAffected).min().orElse(0);
  }

  @Override
  public @NotNull String asString() {
    return "intersect(" + String.join(", ", Collections2.transform(regions, Region::asString) + ")");
  }

  @Override
  public @Nullable Box minContainingBox() {
    final List<@Nullable Box> maxContainingBoxes = regions.stream().map(Region::minContainingBox).toList();
    final double minX = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.POSITIVE_INFINITY : value.minX).max().orElse(Double.POSITIVE_INFINITY);
    final double minY = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.POSITIVE_INFINITY : value.minY).max().orElse(Double.POSITIVE_INFINITY);
    final double minZ = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.POSITIVE_INFINITY : value.minZ).max().orElse(Double.POSITIVE_INFINITY);
    final double maxX = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.NEGATIVE_INFINITY : value.maxX).min().orElse(Double.NEGATIVE_INFINITY);
    final double maxY = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.NEGATIVE_INFINITY : value.maxY).min().orElse(Double.NEGATIVE_INFINITY);
    final double maxZ = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.NEGATIVE_INFINITY : value.maxZ).min().orElse(Double.NEGATIVE_INFINITY);
    if (minX > maxX || minY > maxY || minZ > maxZ) {
      return null;
    } else {
      return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
  }

  public enum Type implements RegionType<IntersectRegion> {
    INTERSECT_TYPE;

    @Override
    public @Nullable RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<IntersectRegion>> {
    private final List<RegionArgument<?>> regions = new ArrayList<>();

    @Override
    public @NotNull String functionName() {
      return "intersect";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.intersect");
    }

    @Override
    public RegionArgument<IntersectRegion> getParseResult(SuggestedParser parser) {
      return source -> new IntersectRegion(regions.stream().map(regionArgument -> (Region) regionArgument.toAbsoluteRegion(source)).toList());
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      regions.add(RegionArgument.parse(commandRegistryAccess, parser, suggestionsOnly));
    }
  }
}
