package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.block.FunctionLikeParser;

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

  @Override
  public @NotNull RegionType<?> getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public double volume() {
    return (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
  }

  @Override
  public long numberOfBlocksAffected() {
    return round().numberOfBlocksAffected();
  }

  @Override
  public String asString() {
    return "cuboid(%s %s %s, %s %s %s)".formatted(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
  }

  public enum Type implements RegionType<CuboidRegion> {
    INSTANCE;

    @Override
    public @Nullable RegionArgument<CuboidRegion> parse(SuggestedParser parser) throws CommandSyntaxException {
      return new Parser().parse(parser);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<CuboidRegion>> {
    private PosArgument from;
    private PosArgument to;

    @Override
    public @NotNull String functionName() {
      return "cuboid";
    }

    @Override
    public Text tooltip() {
      return null; // TODO: 2023/5/13, 013 tooltip
    }

    @Override
    public RegionArgument<CuboidRegion> getParseResult() {
      return source -> new CuboidRegion(from.toAbsolutePos(source), to.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex) throws CommandSyntaxException {
      parser.suggestions.add((builder, context) -> Vec3ArgumentType.vec3().listSuggestions(context, builder));
      if (paramIndex == 0) {
        from = Vec3ArgumentType.vec3().parse(parser.reader);
      } else if (paramIndex == 1) {
        to = Vec3ArgumentType.vec3().parse(parser.reader);
      }
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }
  }
}
