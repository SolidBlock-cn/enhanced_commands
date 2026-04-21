package pers.solid.ecmd.region;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record IntersectRegion(List<Region> regions) implements RegionsBasedRegion<IntersectRegion, Region> {
  public static final MapCodec<IntersectRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(RegionsBasedRegion.regionsCodecField(Region.CODEC)).apply(i, IntersectRegion::new));

  @Override
  public boolean contains(Vec3 vec3d) {
    return regions.stream().allMatch(region -> region.contains(vec3d));
  }

  @Override
  public boolean contains(Vec3i vec3i) {
    return regions.stream().allMatch(region -> region.contains(vec3i));
  }

  @Override
  public Iterator<BlockPos> iterator() {
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
  public RegionType<IntersectRegion> getType() {
    return RegionTypes.INTERSECT;
  }

  /**
   * The volume of the intersection region is inaccurate. The actual probability equals to of lower than it.
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
  public String expressAsString() {
    return "intersect(" + String.join(", ", Collections2.transform(regions, Region::expressAsString)) + ")";
  }

  @Override
  public @Nullable AABB minContainingBox() {
    final List<@Nullable AABB> maxContainingBoxes = regions.stream().map(Region::minContainingBox).toList();
    final double minX = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.POSITIVE_INFINITY : value.minX).max().orElse(Double.POSITIVE_INFINITY);
    final double minY = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.POSITIVE_INFINITY : value.minY).max().orElse(Double.POSITIVE_INFINITY);
    final double minZ = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.POSITIVE_INFINITY : value.minZ).max().orElse(Double.POSITIVE_INFINITY);
    final double maxX = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.NEGATIVE_INFINITY : value.maxX).min().orElse(Double.NEGATIVE_INFINITY);
    final double maxY = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.NEGATIVE_INFINITY : value.maxY).min().orElse(Double.NEGATIVE_INFINITY);
    final double maxZ = maxContainingBoxes.stream().mapToDouble(value -> value == null ? Double.NEGATIVE_INFINITY : value.maxZ).min().orElse(Double.NEGATIVE_INFINITY);
    if (minX > maxX || minY > maxY || minZ > maxZ) {
      return null;
    } else {
      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
  }

  @Override
  public IntersectRegion newRegion(List<Region> regions) {
    return new IntersectRegion(regions);
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<IntersectRegionProvider> {
    private final List<RegionProvider<?>> regions = new ArrayList<>();

    @Override
    public IntersectRegionProvider getParseResult(ParseContext<?> parseContext) {
      return new IntersectRegionProvider(regions);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      regions.add(RegionProvider.parse(parseContext));
    }
  }
}
