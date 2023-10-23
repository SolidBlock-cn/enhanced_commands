package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * 基于现成的区域并进行特定的映射的区域。
 */
public interface MappingRegion extends RegionBasedRegion<MappingRegion, Region> {
  Vec3d getMappedPosOf(Vec3d original);

  Vec3d getOriginalPosOf(Vec3d mapped);

  default Vec3i getMappedPosOf(Vec3i original) {
    return BlockPos.ofFloored(getMappedPosOf(Vec3d.ofCenter(original)));
  }

  default Vec3i getOriginalPosOf(Vec3i mapped) {
    return BlockPos.ofFloored(getMappedPosOf(Vec3d.ofCenter(mapped)));
  }

  @Override
  default boolean contains(@NotNull Vec3i vec3i) {
    return region().contains(getOriginalPosOf(vec3i));
  }

  @Override
  default boolean contains(@NotNull Vec3d vec3d) {
    return region().contains(getOriginalPosOf(vec3d));
  }

  @Override
  @NotNull
  default Iterator<BlockPos> iterator() {
    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    return Iterators.transform(region().iterator(), input -> mutable.set(getMappedPosOf(input)));
  }

  @Override
  default Stream<BlockPos> stream() {
    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    return region().stream().map(blockPos -> mutable.set(getMappedPosOf(blockPos)));
  }
}
