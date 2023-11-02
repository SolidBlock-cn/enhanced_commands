package pers.solid.ecmd.region;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.*;
import java.util.stream.Stream;

public record UnionRegion(Collection<Region> regions) implements RegionsBasedRegion<UnionRegion, Region> {
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

  @Override
  public @Nullable Box minContainingBox() {
    final List<@NotNull Box> maxContainingBoxes = regions.stream().map(Region::minContainingBox).filter(Objects::nonNull).toList();
    final double minX = maxContainingBoxes.stream().mapToDouble(value -> value.minX).min().orElse(Double.POSITIVE_INFINITY);
    final double minY = maxContainingBoxes.stream().mapToDouble(value -> value.minY).min().orElse(Double.POSITIVE_INFINITY);
    final double minZ = maxContainingBoxes.stream().mapToDouble(value -> value.minZ).min().orElse(Double.POSITIVE_INFINITY);
    final double maxX = maxContainingBoxes.stream().mapToDouble(value -> value.maxX).max().orElse(Double.NEGATIVE_INFINITY);
    final double maxY = maxContainingBoxes.stream().mapToDouble(value -> value.maxY).max().orElse(Double.NEGATIVE_INFINITY);
    final double maxZ = maxContainingBoxes.stream().mapToDouble(value -> value.maxZ).max().orElse(Double.NEGATIVE_INFINITY);
    if (minX > maxX || minY > maxY || minZ > maxZ) {
      return null;
    } else {
      return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
  }

  @Override
  public UnionRegion newRegion(Collection<Region> regions) {
    return new UnionRegion(regions);
  }

  public enum Type implements RegionType<UnionRegion> {
    UNION_TYPE;

    @Override
    public @Nullable RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
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
      return Text.translatable("enhanced_commands.argument.region.union");
    }

    @Override
    public RegionArgument<UnionRegion> getParseResult(SuggestedParser parser) {
      return source -> new UnionRegion(regions.stream().map(regionArgument -> (Region) regionArgument.toAbsoluteRegion(source)).collect(ImmutableList.toImmutableList()));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      regions.add(RegionArgument.parse(commandRegistryAccess, parser, suggestionsOnly));
    }
  }
}
