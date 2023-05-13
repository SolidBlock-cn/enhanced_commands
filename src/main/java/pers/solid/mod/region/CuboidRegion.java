package pers.solid.mod.region;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record CuboidRegion(Box box) implements Region {
  public CuboidRegion(double x1, double y1, double z1, double x2, double y2, double z2) {
    this(new Box(x1, y1, z1, x2, y2, z2));
  }

  public CuboidRegion(Vec3d fromPos, Vec3d toPos) {
    this(new Box(fromPos, toPos));
  }

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return box.contains(vec3d);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return round().iterator();
  }

  public BlockCuboidRegion round() {
    return new BlockCuboidRegion((int) Math.round(box.minX), (int) Math.round(box.minY), (int) Math.round(box.minZ), (int) Math.round(box.maxX), (int) Math.round(box.maxY), (int) Math.round(box.maxZ));
  }

  @Override
  public @NotNull Region moved(@NotNull Vec3d relativePos) {
    return new CuboidRegion(new Vec3d(box.minX, box.minY, box.minZ).add(relativePos), new Vec3d(box.maxZ, box.maxY, box.maxZ).add(relativePos));
  }

  @Override
  public @NotNull Region rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    throw new UnsupportedOperationException(); // TODO: 2023/5/6, 006  rotate cuboid
  }

  @Override
  public @NotNull Region mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    throw new UnsupportedOperationException(); // TODO: 2023/5/6, 006  mirror cuboid
  }
}
