package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;

import java.util.Iterator;
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
  public @NotNull Type getType() {
    return RegionTypes.CUBOID_WALL;
  }

  @Override
  public @NotNull CuboidWallRegion rotated(@NotNull Vec3i pivot, @NotNull BlockRotation blockRotation) {
    final BlockCuboidRegion rotated = blockCuboidRegion.rotated(pivot, blockRotation);
    return rotated.equals(blockCuboidRegion) ? this : new CuboidWallRegion(rotated, thickness);
  }

  @Override
  public @NotNull CuboidWallRegion mirrored(Vec3i pivot, Direction.@NotNull Axis axis) {
    final BlockCuboidRegion mirrored = blockCuboidRegion.mirrored(pivot, axis);
    return mirrored.equals(blockCuboidRegion) ? this : new CuboidWallRegion(mirrored, thickness);
  }

  @Override
  public long numberOfBlocksAffected() {
    final int innerBlocks;
    if (blockCuboidRegion.minX() + thickness > blockCuboidRegion.maxX() - thickness || blockCuboidRegion.minZ() + thickness > blockCuboidRegion.maxZ() + thickness) {
      innerBlocks = 0;
    } else {
      innerBlocks = (blockCuboidRegion.maxX() - blockCuboidRegion.minX() - 2 * thickness + 1) * (blockCuboidRegion.maxY() - blockCuboidRegion.minY() + 1) * (blockCuboidRegion.maxZ() - blockCuboidRegion.minZ() - 2 * thickness + 1);
    }
    return blockCuboidRegion.numberOfBlocksAffected() - innerBlocks;
  }

  @Override
  public @NotNull BlockBox maxContainingBlockBox() {
    return blockCuboidRegion.maxContainingBlockBox();
  }

  @Override
  public @NotNull String asString() {
    return String.format("cuboid_wall(%s %s %s, %s %s %s, %s)", blockCuboidRegion.minX(), blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.maxX(), blockCuboidRegion.maxY(), blockCuboidRegion.maxZ(), thickness);
  }

  @Override
  public @Nullable Box maxContainingBox() {
    return blockCuboidRegion.maxContainingBox();
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.concat(Iterators.transform(decompose().iterator(), BlockCuboidRegion::iterator));
  }

  @Override
  public Stream<BlockPos> stream() {
    return decompose().flatMap(Region::stream);
  }

  public @NotNull Stream<BlockCuboidRegion> decompose() {
    // 考虑正好中间的空间为零的情况，这种情况下，正好相当于实心的 BlockCuboidRegion
    if (blockCuboidRegion.minX() + thickness > blockCuboidRegion.maxX() - thickness
        || blockCuboidRegion.minZ() + thickness > blockCuboidRegion.maxZ() - thickness) {
      return Stream.of(blockCuboidRegion);
    }
    return Stream.of(
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
    public @Nullable RegionArgument<CuboidWallRegion> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
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
    public RegionArgument<CuboidWallRegion> getParseResult(SuggestedParser parser) {
      return source -> new CuboidWallRegion(new BlockCuboidRegion(fromPos.toAbsoluteBlockPos(source), toPos.toAbsoluteBlockPos(source)), thickness);
    }
  }
}
