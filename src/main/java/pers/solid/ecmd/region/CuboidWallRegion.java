package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record CuboidWallRegion(BlockCuboidRegion blockCuboidRegion, int thickness) implements IntBackedRegion {
  public CuboidWallRegion {
    if (thickness <= 0) {
      throw new IllegalArgumentException(CuboidOutlineRegion.NON_POSITIVE_THICKNESS.create(thickness));
    }
    final int maxAcceptableThickness = getMaxAcceptableThickness(blockCuboidRegion);
    if (thickness > maxAcceptableThickness) {
      throw new IllegalArgumentException(CuboidOutlineRegion.TOO_THICK.create(maxAcceptableThickness, thickness));
    }
  }

  public static int getMaxAcceptableThickness(BlockCuboidRegion blockCuboidRegion) {
    return Math.min(Math.floorDiv(blockCuboidRegion.maxX() - blockCuboidRegion.minX() + 1, 2), Math.floorDiv(blockCuboidRegion.maxZ() - blockCuboidRegion.minZ() + 1, 2));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return blockCuboidRegion.contains(vec3i) && !blockCuboidRegion.expanded(-thickness, Direction.Axis.X).expanded(-thickness, Direction.Axis.Z).contains(vec3i);
  }

  @Override
  public @NotNull CuboidWallRegion moved(@NotNull Vec3i relativePos) {
    return new CuboidWallRegion(blockCuboidRegion.moved(relativePos), thickness);
  }

  @Override
  public @NotNull RegionType<CuboidWallRegion> getType() {
    return RegionTypes.CUBOID_WALL;
  }

  @Override
  public @NotNull CuboidWallRegion rotated(@NotNull Vec3i center, @NotNull BlockRotation blockRotation) {
    return null;
  }

  @Override
  public @NotNull CuboidWallRegion mirrored(Vec3i center, Direction.@NotNull Axis axis) {
    return null;
  }

  @Override
  public long numberOfBlocksAffected() {
    return blockCuboidRegion.numberOfBlocksAffected() - blockCuboidRegion.expanded(-thickness, Direction.Axis.X).expanded(-thickness, Direction.Axis.Z).numberOfBlocksAffected();
  }

  @Override
  public @NotNull String asString() {
    return String.format("cuboid_wall(%s %s %s, %s %s %s, %s)", blockCuboidRegion.minX(), blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.maxX(), blockCuboidRegion.maxY(), blockCuboidRegion.maxZ(), thickness);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.concat(Iterators.transform(decompose().iterator(), BlockCuboidRegion::iterator));
  }

  @Override
  public Stream<BlockPos> stream() {
    return decompose().stream().flatMap(Region::stream);
  }

  public @NotNull List<BlockCuboidRegion> decompose() {
    return List.of(
        // lower x part
        new BlockCuboidRegion(blockCuboidRegion.minX(), blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.minX() + thickness - 1, blockCuboidRegion.maxY(), blockCuboidRegion.maxZ()),
        // higher x part
        new BlockCuboidRegion(blockCuboidRegion.maxX() - thickness + 1, blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.maxX(), blockCuboidRegion.maxY(), blockCuboidRegion.maxZ()),
        // lower z part
        new BlockCuboidRegion(blockCuboidRegion.minX() + thickness, blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.maxX() - thickness, blockCuboidRegion.maxY(), blockCuboidRegion.minZ() + thickness - 1),
        // higher z part
        new BlockCuboidRegion(blockCuboidRegion.minX() + thickness, blockCuboidRegion.minY(), blockCuboidRegion.maxZ() - thickness + 1, blockCuboidRegion.maxX() - thickness, blockCuboidRegion.maxY(), blockCuboidRegion.maxZ())
    );
  }

  public enum Type implements RegionType<CuboidWallRegion> {
    CUBOID_WALL_TYPE;

    @Override
    public @Nullable RegionArgument<CuboidWallRegion> parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(parser, suggestionsOnly);
    }
  }

  public static final class Parser extends CuboidOutlineRegion.AbstractParser<CuboidWallRegion> {
    @Override
    public @NotNull String functionName() {
      return "cuboid_wall";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.cuboid_wall");
    }

    @Override
    public RegionArgument<CuboidWallRegion> getParseResult() {
      return source -> new CuboidWallRegion(new BlockCuboidRegion(fromPos.toAbsoluteBlockPos(source), toPos.toAbsoluteBlockPos(source)), thickness);
    }
  }
}
