package pers.solid.ecmd.region;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record UnionRegion(@NotNull List<Region> regions) implements RegionsBasedRegion<UnionRegion, Region> {
  public static final MapCodec<UnionRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(RegionsBasedRegion.regionsCodecField(Region.CODEC)).apply(i, UnionRegion::new));

  @Override
  public boolean contains(@NotNull Vec3 vec3d) {
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
  public Stream<@NotNull BlockPos> stream() {
    return regions.stream().flatMap(Region::stream).map(BlockPos::immutable).distinct();
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.UNION;
  }

  /**
   * The probability is inaccurate. The actual probability equals or is lower than it.
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
  public @Nullable AABB minContainingBox() {
    final List<@NotNull AABB> maxContainingBoxes = regions.stream().map(Region::minContainingBox).filter(Objects::nonNull).toList();
    final double minX = maxContainingBoxes.stream().mapToDouble(value -> value.minX).min().orElse(Double.POSITIVE_INFINITY);
    final double minY = maxContainingBoxes.stream().mapToDouble(value -> value.minY).min().orElse(Double.POSITIVE_INFINITY);
    final double minZ = maxContainingBoxes.stream().mapToDouble(value -> value.minZ).min().orElse(Double.POSITIVE_INFINITY);
    final double maxX = maxContainingBoxes.stream().mapToDouble(value -> value.maxX).max().orElse(Double.NEGATIVE_INFINITY);
    final double maxY = maxContainingBoxes.stream().mapToDouble(value -> value.maxY).max().orElse(Double.NEGATIVE_INFINITY);
    final double maxZ = maxContainingBoxes.stream().mapToDouble(value -> value.maxZ).max().orElse(Double.NEGATIVE_INFINITY);
    if (minX > maxX || minY > maxY || minZ > maxZ) {
      return null;
    } else {
      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
  }

  @Override
  public UnionRegion newRegion(@NotNull List<Region> regions) {
    return new UnionRegion(regions);
  }

  public enum Type implements RegionType<UnionRegion> {
    UNION_TYPE;

    @Override
    public String functionName() {
      return "union";
    }

    @Override
    public Component tooltip() {
      return Component.translatable("enhanced_commands.region.union");
    }

    @Override
    public FunctionLikeParser.SequentialParams<UnionRegionProvider> parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<UnionRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionProvider<UnionRegion>> getArgumentCodec() {
      return UnionRegionProvider.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.SequentialParams<UnionRegionProvider> {
    private final List<RegionProvider<?>> regions = new ArrayList<>();

    @Override
    public UnionRegionProvider getParseResult(ParseContext<?> parseContext) {
      return new UnionRegionProvider(regions);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      regions.add(RegionProvider.parse(parseContext));
    }
  }
}
