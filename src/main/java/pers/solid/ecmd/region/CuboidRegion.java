package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Collections;
import java.util.Iterator;

/**
 * <p>A <b>cuboid region</b> is a region representing a cuboid, which is defined by two positions. The positions are accurate positions, instead of block positions. The coordinates can be decimal, even if blocks only support non-decimal coordinates.
 * <p><b>Syntax:</b> {@code cuboid(<begin pos>, <end pos>)}
 * <p>As the positions are accurate positions, the two points must have some distance in several coordinates. For example, {@code cuboid(0.0 0.0 0.0, 0.0 0.0 0.0)} does not contain any area. Instead, {@code cuboid(0.0 0.0 0.0, 1.0 1.0 1.0)} is a region contain a <em>block pos</em> {@code 0 0 0}. It is identical to <em>block cuboid region</em> {@code cuboid(0 0 0, 0 0 0)}.
 * <p>Whether a block position is in the cuboid regions is determined by whether the accurate center position of the block pos is in the cuboid region. For example, {@code cuboid(0.4 0.4 0.4, 1.2 1.2 1.2)} contains block pos {@code (0 0 0)} (whose center pos is {@code (0.5 0.5 0.5)}), but does not contain {@code (1 1 1)} (whose center pos is {@code (1.5 1.5 1.5)}).
 *
 * @param box
 */
public record CuboidRegion(Box box) implements Region {
  /**
   * Create a cuboid region from several coordinates. The comparison is not required because it will be compared in implementation.
   */
  public CuboidRegion(double x1, double y1, double z1, double x2, double y2, double z2) {
    this(new Box(x1, y1, z1, x2, y2, z2));
  }

  /**
   * Create a cuboid region from two positions. The relative relation is not required because the coordinates will be compared in implementation.
   */
  public CuboidRegion(Vec3d fromPos, Vec3d toPos) {
    this(new Box(fromPos, toPos));
  }

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return box.contains(vec3d);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    final BlockCuboidRegion round = round();
    return round == null ? Collections.emptyIterator() : round.iterator();
  }

  /**
   * Round the cuboid region into a block cuboid region, in which each block position's center position is in this cuboid region. It may be {@code null} if the region does not contain any block.
   */
  public @Nullable BlockCuboidRegion round() {
    final int minX = (int) Math.round(box.minX);
    final int minY = (int) Math.round(box.minY);
    final int minZ = (int) Math.round(box.minZ);
    final int maxX = (int) Math.round(box.maxX);
    final int maxY = (int) Math.round(box.maxY);
    final int maxZ = (int) Math.round(box.maxZ);
    if (minX == maxX || minY == maxX || minZ == maxX) {
      return null;
    }
    return new BlockCuboidRegion(minX, minY, minZ, maxX - 1, maxY - 1, maxZ - 1);
  }

  @Override
  public @NotNull Region moved(@NotNull Vec3d relativePos) {
    return new CuboidRegion(new Vec3d(box.minX, box.minY, box.minZ).add(relativePos), new Vec3d(box.maxZ, box.maxY, box.maxZ).add(relativePos));
  }

  @Override
  public @NotNull Region rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return new CuboidRegion(
        GeoUtil.rotate(new Vec3d(box.minX, box.minY, box.minZ), blockRotation, pivot),
        GeoUtil.rotate(new Vec3d(box.maxX, box.maxY, box.maxZ), blockRotation, pivot)
    );
  }

  @Override
  public @NotNull Region mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return new CuboidRegion(
        GeoUtil.mirror(new Vec3d(box.minX, box.minY, box.minZ), axis, pivot),
        GeoUtil.mirror(new Vec3d(box.maxX, box.maxY, box.maxZ), axis, pivot)
    );
  }

  @Override
  public @NotNull Region expanded(double offset) {
    return new CuboidRegion(box.expand(offset));
  }

  @Override
  public @NotNull Region expanded(double offset, Direction.Axis axis) {
    var x = axis.choose(offset, 0, 0);
    var y = axis.choose(0, offset, 0);
    var z = axis.choose(0, 0, offset);
    return new CuboidRegion(box.expand(x, y, z));
  }

  @Override
  public @NotNull Region expanded(double offset, Direction direction) {
    if (offset > 0) {
      return new CuboidRegion(box.stretch(Vec3d.of(direction.getVector()).multiply(offset)));
    } else if (offset < 0) {
      var vec = Vec3d.of(direction.getVector()).multiply(offset);
      return new CuboidRegion(box.shrink(vec.x, vec.y, vec.z));
    } else {
      return this;
    }
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
    final BlockCuboidRegion round = round();
    return round == null ? 0 : round.numberOfBlocksAffected();
  }

  @Override
  public @NotNull String asString() {
    return "cuboid(%s %s %s, %s %s %s)".formatted(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
  }

  @Override
  public @Nullable Box minContainingBox() {
    return box;
  }

  public enum Type implements RegionType<CuboidRegion> {
    CUBOID_TYPE;

    @Override
    public @Nullable RegionArgument<Region> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<Region>> {
    private PosArgument from;
    private PosArgument to;

    @Override
    public @NotNull String functionName() {
      return "cuboid";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.cuboid");
    }

    @Override
    public RegionArgument<Region> getParseResult(SuggestedParser parser) {
      if (EnhancedPosArgument.isInt(from) && EnhancedPosArgument.isInt(to)) {
        return source -> new BlockCuboidRegion(from.toAbsoluteBlockPos(source), to.toAbsoluteBlockPos(source));
      }
      return source -> new CuboidRegion(from.toAbsolutePos(source), to.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
      if (paramIndex == 0) {
        from = ParsingUtil.suggestParserFromType(type, parser, suggestionsOnly);
        if (parser.reader.canRead() && Character.isWhitespace(parser.reader.peek())) {
          parser.reader.skipWhitespace();
          // 在有接受到空格后，可直接接受第二个参数
          if (parser.reader.canRead()) {
            final char peek = parser.reader.peek();
            if (peek != ',' && peek != ')') {
              to = ParsingUtil.suggestParserFromType(type, parser, suggestionsOnly);
            }
          }
        }
      } else if (paramIndex == 1) {
        to = ParsingUtil.suggestParserFromType(type, parser, suggestionsOnly);
      }
    }

    @Override
    public int minParamsCount() {
      return to != null ? 1 : 2;
    }

    @Override
    public int maxParamsCount() {
      // 如果接受到了以空格区分的参数，那么不需要接受第二个参数了。
      return to != null ? 1 : 2;
    }
  }
}
