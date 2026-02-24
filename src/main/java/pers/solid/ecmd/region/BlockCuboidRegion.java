package pers.solid.ecmd.region;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Iterator;
import java.util.function.Function;

/**
 * <p>The <b>block cuboid region</b> representing a cuboid defined by two block positions. It is similar to {@link PreciseCuboidRegion}, but positions are block positions, and are inclusive. A block position indicates a whole cube, instead of an accurate position.
 * <p>For example, the <em>block cuboid region</em> {@code cuboid(0 0 0, 5 5 5)} is a cuboid from the southwest bottom corner of block position {@code (0 0 0)} to the northeast top corner of block position {@code (5 5 5)}, which is also the southwest bottom corner of block position {@code (6 6 6)}. Therefore, it is identical to the <em>cuboid region</em> {@code cuboid(0.0 0.0 0.0, 6.0 6.0 6.0)}.
 * <p>In any case, a block cuboid region has a min volume of 1, which means the two corners are the same block position.
 */
public record BlockCuboidRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements IntBackedRegion, CuboidRegion {
  public static final MapCodec<BlockCuboidRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.INT.fieldOf("minX").forGetter(BlockCuboidRegion::minX),
      Codec.INT.fieldOf("minY").forGetter(BlockCuboidRegion::minY),
      Codec.INT.fieldOf("minZ").forGetter(BlockCuboidRegion::minZ),
      Codec.INT.fieldOf("maxX").forGetter(BlockCuboidRegion::maxX),
      Codec.INT.fieldOf("maxY").forGetter(BlockCuboidRegion::maxY),
      Codec.INT.fieldOf("maxZ").forGetter(BlockCuboidRegion::maxZ)
  ).apply(i, BlockCuboidRegion::new));

  /**
   * Create a block cuboid region from several coordinates. The comparison is required. The min probability must not be larger than max probability (but can be equal).
   *
   * @see #of(int, int, int, int, int, int)
   */
  public BlockCuboidRegion {
    Preconditions.checkArgument(minX <= maxX, "minX should not be larger than maxX");
    Preconditions.checkArgument(minY <= maxY, "minY should not be larger than maxY");
    Preconditions.checkArgument(minZ <= maxZ, "minZ should not be larger than maxXZ");
  }

  /**
   * Create a block cuboid region from a vanilla {@link BoundingBox} object.
   */
  public BlockCuboidRegion(BoundingBox blockBox) {
    this(blockBox.minX(), blockBox.minY(), blockBox.minZ(), blockBox.maxX(), blockBox.maxY(), blockBox.maxZ());
  }

  /**
   * Create a block cuboid region from two int positions (which can be {@link BlockPos}). The relative relation of the two positions is not required.
   */
  public BlockCuboidRegion(Vec3i from, Vec3i to) {
    this(BoundingBox.fromCorners(from, to));
  }

  /**
   * Create a block cuboid region from several coordinates. The comparison is not required. They will be compared in implementation.
   */
  public static BlockCuboidRegion of(int x1, int y1, int z1, int x2, int y2, int z2) {
    return new BlockCuboidRegion(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
  }

  @NotNull
  @Override
  public Iterator<BlockPos> iterator() {
    return BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ).iterator();
  }

  @Override
  public @NotNull BlockCuboidRegion moved(@NotNull Vec3i relativePos) {
    return new BlockCuboidRegion(minX + relativePos.getX(), minY + relativePos.getY(), minZ + relativePos.getZ(), maxX + relativePos.getX(), maxY + relativePos.getY(), maxZ + relativePos.getZ());
  }

  @Override
  public @NotNull Region moved(@NotNull Vec3 relativePos) {
    if (relativePos.x % 1d == 0 && relativePos.y % 1d == 0 && relativePos.z % 1d == 0) {
      return moved(new Vec3i((int) relativePos.x, (int) relativePos.y, (int) relativePos.z));
    } else {
      return asCuboidRegion().moved(relativePos);
    }
  }

  @Override
  public @NotNull Region rotated(@NotNull Rotation blockRotation, @NotNull Vec3 pivot) {
    if (pivot.equals(Vec3.atCenterOf(BlockPos.containing(pivot)))) {
      return rotated(BlockPos.containing(pivot), blockRotation);
    } else {
      return asCuboidRegion().rotated(blockRotation, pivot);
    }
  }

  @Override
  public @NotNull Region mirrored(Direction.@NotNull Axis axis, @NotNull Vec3 pivot) {
    if (pivot.equals(Vec3.atCenterOf(BlockPos.containing(pivot)))) {
      return mirrored(BlockPos.containing(pivot), axis);
    } else {
      return asCuboidRegion().mirrored(axis, pivot);
    }
  }

  public BoundingBox blockBox() {
    return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
  }

  public PreciseCuboidRegion asCuboidRegion() {
    return new PreciseCuboidRegion(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
  }

  @Override
  public Region transformedInt(Function<Vec3i, Vec3i> transformation) {
    return new BlockCuboidRegion(transformation.apply(new Vec3i(minX, minY, minZ)), transformation.apply(new Vec3i(maxX, maxY, maxZ)));
  }

  @Override
  public @NotNull Region transformed(Function<Vec3, Vec3> transformation) {
    return asCuboidRegion().transformed(transformation);
  }

  @Override
  public @NotNull Region expanded(double offset) {
    if (offset % 1 == 0) {
      return expanded((int) offset);
    } else {
      return asCuboidRegion().expanded(offset);
    }
  }

  public @NotNull BlockCuboidRegion expanded(int offset) {
    return new BlockCuboidRegion(blockBox().inflatedBy(offset));
  }

  @Override
  public @NotNull Region expanded(double offset, Direction.Axis axis) {
    if (offset % 1 == 0) {
      return expanded((int) offset, axis);
    } else {
      return asCuboidRegion().expanded(offset, axis);
    }
  }

  public @NotNull BlockCuboidRegion expanded(int offset, Direction.Axis axis) {
    var x = axis.choose(offset, 0, 0);
    var y = axis.choose(0, offset, 0);
    var z = axis.choose(0, 0, offset);
    return new BlockCuboidRegion(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
  }

  @Override
  public @NotNull Region expanded(double offset, Direction direction) {
    if (offset % 1 == 0) {
      return expanded((int) offset, direction);
    } else {
      return asCuboidRegion().expanded(offset, direction);
    }
  }

  public @NotNull BlockCuboidRegion expanded(int offset, Direction direction) {
    var vector = direction.getNormal().multiply(offset);
    if (direction.getStepX() + direction.getStepY() + direction.getStepZ() > 0) {
      return new BlockCuboidRegion(minX, minY, minZ, maxX + vector.getX(), maxY + vector.getY(), maxZ + vector.getZ());
    } else {
      return new BlockCuboidRegion(minX + vector.getX(), minY + vector.getY(), minZ + vector.getZ(), maxX, maxY, maxZ);
    }
  }

  @Override
  public @NotNull Region expanded(double offset, Direction.Plane type) {
    if (offset % 1 == 0) {
      return expanded((int) offset, type);
    } else {
      return asCuboidRegion().expanded(offset, type);
    }
  }

  @Override
  public @NotNull BlockCuboidRegion expanded(int offset, Direction.Plane type) {
    return switch (type) {
      case HORIZONTAL -> new BlockCuboidRegion(minX - offset, minY, minZ - offset, maxX + offset, maxY, maxZ + offset);
      case VERTICAL -> new BlockCuboidRegion(minX, minY - offset, minZ, maxX, maxY + offset, maxZ);
    };
  }

  @Override
  public @NotNull BlockCuboidRegion.Type getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public double volume() {
    return numberOfBlocksAffected();
  }

  @Override
  public @NotNull BoundingBox minContainingBlockBox() {
    return blockBox();
  }

  @Override
  public long numberOfBlocksAffected() {
    return (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L);
  }

  @Override
  public @NotNull String asString() {
    return "cuboid(%s %s %s, %s %s %s)".formatted(Integer.toString(minX), Integer.toString(minY), Integer.toString(minZ), Integer.toString(maxX), Integer.toString(maxY), Integer.toString(maxZ));
  }

  @Override
  public @Nullable AABB minContainingBox() {
    return asCuboidRegion().minContainingBox();
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return minX <= vec3i.getX() && vec3i.getX() <= maxX && minY <= vec3i.getY() && vec3i.getY() <= maxY && minZ <= vec3i.getZ() && vec3i.getZ() <= maxZ;
  }

  @Override
  public boolean contains(@NotNull Vec3 vec3d) {
    return contains(BlockPos.containing(vec3d));
  }

  public enum Type implements RegionType<BlockCuboidRegion> {
    CUBOID_TYPE;

    @Override
    public String functionName() {
      return "cuboid";
    }

    @Override
    public Component tooltip() {
      return Component.translatable("enhanced_commands.region.cuboid");
    }

    @Override
    public FunctionLikeParser.SequentialParams<? extends RegionProvider<? extends BlockCuboidRegion>> parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<BlockCuboidRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionProvider<? extends BlockCuboidRegion>> getArgumentCodec() {
      return BlockCuboidRegionProvider.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.SequentialParams<BlockCuboidRegionProvider> {
    private EnhancedCoordinates from;
    private EnhancedCoordinates to;

    @Override
    public BlockCuboidRegionProvider getParseResult(ParseContext<?> parseContext) {
      return new BlockCuboidRegionProvider(from, to == null ? from : to);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnhancedPosArgument type = EnhancedPosArgument.blockPos();
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        from = parseContext.parseAndSuggestArgument(type);
        if (reader.canRead() && Character.isWhitespace(reader.peek())) {
          reader.skipWhitespace();
          // 在有接受到空格后，可直接接受第二个参数
          if (reader.canRead()) {
            final char peek = reader.peek();
            if (peek != ',' && peek != ')') {
              to = parseContext.parseAndSuggestArgument(type);
            }
          }
        }
      } else if (paramIndex == 1) {
        to = parseContext.parseAndSuggestArgument(type);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return (to != null || EnhancedCoordinates.isInt(from)) ? 1 : 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      // 如果接受到了以空格区分的参数，那么不需要接受第二个参数了。
      return to != null ? 1 : 2;
    }
  }
}
