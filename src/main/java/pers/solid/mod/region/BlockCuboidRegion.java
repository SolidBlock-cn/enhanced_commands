package pers.solid.mod.region;

import com.google.common.base.Preconditions;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record BlockCuboidRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements Region {
  public BlockCuboidRegion {
    Preconditions.checkArgument(minX <= maxX, "minX should not be larger than maxX");
    Preconditions.checkArgument(minY <= maxY, "minY should not be larger than maxY");
    Preconditions.checkArgument(minZ <= maxZ, "minZ should not be larger than maxX");
  }

  public static BlockCuboidRegion of(int x1, int y1, int z1, int x2, int y2, int z2) {
    return new BlockCuboidRegion(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
  }

  public BlockCuboidRegion(BlockBox blockBox) {
    this(blockBox.getMinX(), blockBox.getMinY(), blockBox.getMinZ(), blockBox.getMaxX(), blockBox.getMaxY(), blockBox.getMaxZ());
  }

  public BlockCuboidRegion(Vec3i from, Vec3i to) {
    this(BlockBox.create(from, to));
  }

  @NotNull
  @Override
  public Iterator<BlockPos> iterator() {
    return BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ).iterator();
  }

  @Override
  public @NotNull BlockCuboidRegion moved(@NotNull Vec3i relativePos) {
    return new BlockCuboidRegion(minX + relativePos.getX(), minY + relativePos.getY(), minZ + relativePos.getZ(), maxX + relativePos.getX(), maxY + relativePos.getY(), maxZ + relativePos.getZ());
  }

  @Override
  public @NotNull Region moved(@NotNull Vec3d relativePos) {
    if (relativePos.x % 1 == 0 && relativePos.y % 1 == 0 && relativePos.z % 1 == 0) {
      return moved(new Vec3i((int) relativePos.x, (int) relativePos.y, (int) relativePos.z));
    } else {
      return asCuboidRegion().moved(relativePos);
    }
  }

  public BlockBox blockBox() {
    return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
  }

  public CuboidRegion asCuboidRegion() {
    return new CuboidRegion(minX, minY, minZ, maxX, maxY, maxZ);
  }

  @Override
  public @NotNull BlockCuboidRegion rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    throw new UnsupportedOperationException(); // TODO: 2023/5/6, 006  rotate cuboid
  }

  @Override
  public @NotNull Region mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    throw new UnsupportedOperationException(); // TODO: 2023/5/6, 006  mirror cuboid
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return minX <= vec3i.getX() && vec3i.getX() <= maxX && minY <= vec3i.getY() && vec3i.getY() <= maxY && minZ <= vec3i.getZ() && vec3i.getZ() <= maxZ;
  }

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return contains(BlockPos.ofFloored(vec3d));
  }

}
