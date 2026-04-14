package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * 基于现成的区域并进行特定的映射的区域。
 */
public interface MappingRegion extends RegionBasedRegion<MappingRegion, Region> {
  Vec3 getMappedPosOf(Vec3 original);

  Vec3 getOriginalPosOf(Vec3 mapped);

  default Vec3i getMappedPosOf(Vec3i original) {
    return BlockPos.containing(getMappedPosOf(Vec3.atCenterOf(original)));
  }

  default Vec3i getOriginalPosOf(Vec3i mapped) {
    return BlockPos.containing(getMappedPosOf(Vec3.atCenterOf(mapped)));
  }

  @Override
  default boolean contains(Vec3i vec3i) {
    return region().contains(getOriginalPosOf(vec3i));
  }

  @Override
  default boolean contains(Vec3 vec3d) {
    return region().contains(getOriginalPosOf(vec3d));
  }

  @Override
  default Iterator<BlockPos> iterator() {
    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    return Iterators.transform(region().iterator(), input -> mutable.set(getMappedPosOf(input)));
  }

  @Override
  default Stream<BlockPos> stream() {
    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    return region().stream().map(blockPos -> mutable.set(getMappedPosOf(blockPos)));
  }
}
