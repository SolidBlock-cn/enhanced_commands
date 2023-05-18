package pers.solid.ecmd.region;

import com.google.common.base.Preconditions;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * <p>The <b>block cuboid region</b> representing a cuboid defined by two block positions. It is similar to {@link CuboidRegion}, but positions are block positions, and are inclusive. A block position indicates a whole cube, instead of an accurate position.
 * <p>For example, the <em>block cuboid region</em> {@code cuboid(0 0 0, 5 5 5)} is a cuboid from the southwest bottom corner of block position {@code (0 0 0)} to the northeast top corner of block position {@code (5 5 5)}, which is also the southwest bottom corner of block position {@code (6 6 6)}. Therefore, it is identical to the <em>cuboid region</em> {@code cuboid(0.0 0.0 0.0, 6.0 6.0 6.0)}.
 * <p>In any case, a block cuboid region has a minimum volume of 1, which means the two corners are a same block position.
 */
public record BlockCuboidRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements Region {
  /**
   * Create a block cuboid region from several coordinates. The comparison is required. The min value must not be larger than max value (but can be equal).
   *
   * @see #of(int, int, int, int, int, int)
   */
  public BlockCuboidRegion {
    Preconditions.checkArgument(minX <= maxX, "minX should not be larger than maxX");
    Preconditions.checkArgument(minY <= maxY, "minY should not be larger than maxY");
    Preconditions.checkArgument(minZ <= maxZ, "minZ should not be larger than maxX");
  }

  /**
   * Create a block cuboid region from several coordinates. The comparison is not required. They will be compared in implementation.
   */
  public static BlockCuboidRegion of(int x1, int y1, int z1, int x2, int y2, int z2) {
    return new BlockCuboidRegion(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
  }

  /**
   * Create a block cuboid region from a vanilla {@link BlockBox} object.
   */
  public BlockCuboidRegion(BlockBox blockBox) {
    this(blockBox.getMinX(), blockBox.getMinY(), blockBox.getMinZ(), blockBox.getMaxX(), blockBox.getMaxY(), blockBox.getMaxZ());
  }

  /**
   * Create a block cuboid region from two int positions (which can be {@link BlockPos}. The relative relation of the two positions are not required.
   */
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
    if (relativePos.x % 1d == 0 && relativePos.y % 1d == 0 && relativePos.z % 1d == 0) {
      return moved(new Vec3i((int) relativePos.x, (int) relativePos.y, (int) relativePos.z));
    } else {
      return asCuboidRegion().moved(relativePos);
    }
  }

  public BlockBox blockBox() {
    return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
  }

  public CuboidRegion asCuboidRegion() {
    return new CuboidRegion(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
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
  public @NotNull RegionType<?> getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public double volume() {
    return numberOfBlocksAffected();
  }

  @Override
  public long numberOfBlocksAffected() {
    return (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L);
  }

  @Override
  public String asString() {
    return "cuboid(%s %s %s, %s %s %s)".formatted(Integer.toString(minX), Integer.toString(minY), Integer.toString(minZ), Integer.toString(maxX), Integer.toString(maxY), Integer.toString(maxZ));
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
