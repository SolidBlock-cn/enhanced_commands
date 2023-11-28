package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.FunctionParamsParser;

import java.util.Iterator;
import java.util.stream.Stream;

public record CuboidWallRegion(BlockCuboidRegion region, int thickness) implements RegionBasedRegion.IntBacked<CuboidWallRegion, BlockCuboidRegion> {
  public CuboidWallRegion {
    if (thickness <= 0) {
      throw new IllegalArgumentException(CuboidOutlineRegion.NON_POSITIVE_THICKNESS.create(thickness));
    }
    final int maxAcceptableThickness = getMaxAcceptableThickness(region);
    if (thickness > maxAcceptableThickness) {
      throw new IllegalArgumentException(CuboidOutlineRegion.TOO_THICK.create(maxAcceptableThickness, thickness));
    }
  }

  public static int getMaxAcceptableThickness(BlockCuboidRegion blockCuboidRegion) {
    return Math.min(Math.floorDiv(blockCuboidRegion.maxX() - blockCuboidRegion.minX() + 1, 2), Math.floorDiv(blockCuboidRegion.maxZ() - blockCuboidRegion.minZ() + 1, 2));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    try {
      return region.contains(vec3i) && !region.expanded(-thickness, Direction.Type.HORIZONTAL).contains(vec3i);
    } catch (IllegalArgumentException args) {
      // min max wrong
      return true;
    }
  }

  @Override
  public CuboidWallRegion newRegion(BlockCuboidRegion region) {
    return new CuboidWallRegion(region, thickness);
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.CUBOID_WALL;
  }

  @Override
  public long numberOfBlocksAffected() {
    final int innerBlocks;
    if (region.minX() + thickness > region.maxX() - thickness || region.minZ() + thickness > region.maxZ() + thickness) {
      innerBlocks = 0;
    } else {
      innerBlocks = (region.maxX() - region.minX() - 2 * thickness + 1) * (region.maxY() - region.minY() + 1) * (region.maxZ() - region.minZ() - 2 * thickness + 1);
    }
    return region.numberOfBlocksAffected() - innerBlocks;
  }

  @Override
  public @NotNull BlockBox minContainingBlockBox() {
    return region.minContainingBlockBox();
  }

  @Override
  public @NotNull String asString() {
    return String.format("cuboid_wall(%s %s %s, %s %s %s, %s)", region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ(), thickness);
  }

  @Override
  public @Nullable Box minContainingBox() {
    return region.minContainingBox();
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
    if (region.minX() + thickness > region.maxX() - thickness
        || region.minZ() + thickness > region.maxZ() - thickness) {
      return Stream.of(region);
    }
    return Stream.of(
        // lower x part
        new BlockCuboidRegion(region.minX(), region.minY(), region.minZ(), region.minX() + thickness - 1, region.maxY(), region.maxZ()),
        // higher x part
        new BlockCuboidRegion(region.maxX() - thickness + 1, region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ()),
        // lower z part
        new BlockCuboidRegion(region.minX() + thickness, region.minY(), region.minZ(), region.maxX() - thickness, region.maxY(), region.minZ() + thickness - 1),
        // higher z part
        new BlockCuboidRegion(region.minX() + thickness, region.minY(), region.maxZ() - thickness + 1, region.maxX() - thickness, region.maxY(), region.maxZ())
    );
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    region.writeNbt(nbtCompound);
    nbtCompound.putInt("thickness", thickness);
  }

  public enum Type implements RegionType<CuboidWallRegion> {
    CUBOID_WALL_TYPE;

    @Override
    public String functionName() {
      return "cuboid_wall";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.cuboid_wall");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return new Parser();
    }

    @Override
    public @NotNull CuboidWallRegion fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final Region region1 = RegionTypes.CUBOID.fromNbt(nbtCompound, world);
      if (region1 instanceof BlockCuboidRegion blockCuboidRegion) {
        return new CuboidWallRegion(blockCuboidRegion, nbtCompound.getInt("thickness"));
      }
      throw new IllegalArgumentException("Cannot parse cuboid wall region: NBT does not contain a BlockCuboidRegion ({type=cuboid, block=true})");
    }
  }

  public static final class Parser extends CuboidOutlineRegion.AbstractParser {
    @Override
    protected Region createParsedResult(ServerCommandSource source) {
      return new CuboidWallRegion(new BlockCuboidRegion(fromPos.toAbsoluteBlockPos(source), toPos.toAbsoluteBlockPos(source)), thickness);
    }
  }
}
