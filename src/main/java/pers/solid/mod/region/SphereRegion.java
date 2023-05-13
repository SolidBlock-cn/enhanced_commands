package pers.solid.mod.region;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record SphereRegion(Vec3d center, double radius) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return vec3d.isInRange(center, radius);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return new CuboidRegion(center.add(-radius, -radius, -radius), center.add(radius, radius, radius)).iterator();
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

}
