package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;

/**
 * <p>A <b>cuboid region</b> is a region representing a cuboid, which is defined by two positions. The positions are accurate positions, instead of block positions. The coordinates can be decimal, even if blocks only support non-decimal coordinates.
 * <p><b>Syntax:</b> {@code cuboid(<begin pos>, <end pos>)}
 * <p>As the positions are accurate positions, the two points must have some distance in several coordinates. For example, {@code cuboid(0.0 0.0 0.0, 0.0 0.0 0.0)} does not contain any area. Instead, {@code cuboid(0.0 0.0 0.0, 1.0 1.0 1.0)} is a region contain a <em>block pos</em> {@code 0 0 0}. It is identical to <em>block cuboid region</em> {@code cuboid(0 0 0, 0 0 0)}.
 * <p>Whether a block position is in the cuboid regions is determined by whether the accurate center position of the block pos is in the cuboid region. For example, {@code cuboid(0.4 0.4 0.4, 1.2 1.2 1.2)} contains block pos {@code (0 0 0)} (whose center pos is {@code (0.5 0.5 0.5)}), but does not contain {@code (1 1 1)} (whose center pos is {@code (1.5 1.5 1.5)}).
 *
 * @param box
 */
public record PreciseCuboidRegion(AABB box) implements CuboidRegion {
  public static final MapCodec<PreciseCuboidRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3.CODEC.fieldOf("from").forGetter(r -> new Vec3(r.box.minX, r.box.minY, r.box.minZ)), Vec3.CODEC.fieldOf("to").forGetter(r -> new Vec3(r.box.maxX, r.box.maxY, r.box.maxZ))).apply(i, PreciseCuboidRegion::new));

  /**
   * Create a cuboid region from several coordinates. The comparison is not required because it will be compared in implementation.
   */
  public PreciseCuboidRegion(double x1, double y1, double z1, double x2, double y2, double z2) {
    this(new AABB(x1, y1, z1, x2, y2, z2));
  }

  /**
   * Create a cuboid region from two positions. The relative relation is not required because the coordinates will be compared in implementation.
   */
  public PreciseCuboidRegion(Vec3 fromPos, Vec3 toPos) {
    this(new AABB(fromPos, toPos));
  }

  @Override
  public boolean contains(@NotNull Vec3 vec3d) {
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
    final int minX = -(int) Math.round(-box.minX);
    final int minY = -(int) Math.round(-box.minY);
    final int minZ = -(int) Math.round(-box.minZ);
    final int maxX = -(int) Math.round(-box.maxX);
    final int maxY = -(int) Math.round(-box.maxY);
    final int maxZ = -(int) Math.round(-box.maxZ);
    if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
      return null;
    }
    return new BlockCuboidRegion(minX, minY, minZ, maxX - 1, maxY - 1, maxZ - 1);
  }

  @Override
  public @NotNull PreciseCuboidRegion transformed(Function<Vec3, Vec3> transformation) {
    return new PreciseCuboidRegion(transformation.apply(new Vec3(box.minX, box.minY, box.minZ)), transformation.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
  }

  @Override
  public @NotNull Region expanded(double offset) {
    return new PreciseCuboidRegion(box.inflate(offset));
  }

  @Override
  public @NotNull Region expanded(double offset, Direction.Axis axis) {
    var x = axis.choose(offset, 0, 0);
    var y = axis.choose(0, offset, 0);
    var z = axis.choose(0, 0, offset);
    return new PreciseCuboidRegion(box.inflate(x, y, z));
  }

  @Override
  public @NotNull Region expanded(double offset, Direction direction) {
    if (offset > 0) {
      return new PreciseCuboidRegion(box.expandTowards(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(offset)));
    } else if (offset < 0) {
      var vec = Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(offset);
      return new PreciseCuboidRegion(box.contract(vec.x, vec.y, vec.z));
    } else {
      return this;
    }
  }

  @Override
  public @NotNull Region expanded(double offset, Direction.Plane type) {
    if (offset == 0) {
      return this;
    }
    switch (type) {
      case HORIZONTAL -> {
        if (offset > 0) {
          return new PreciseCuboidRegion(box.expandTowards(offset, 0, offset));
        } else {
          return new PreciseCuboidRegion(box.contract(-offset, 0, -offset));
        }
      }
      case VERTICAL -> {
        if (offset > 0) {
          return new PreciseCuboidRegion(box.expandTowards(0, offset, 0));
        } else {
          return new PreciseCuboidRegion(box.contract(0, -offset, 0));
        }
      }
      default -> throw new IllegalStateException("Unexpected probability: " + type);
    }
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.CUBOID_PRECISE;
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
    return "cuboid_precise(%s %s %s, %s %s %s)".formatted(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
  }

  @Override
  public @Nullable AABB minContainingBox() {
    return box;
  }


  public enum Type implements RegionType<PreciseCuboidRegion> {
    PRECISE_CUBOID_TYPE;

    @Override
    public String functionName() {
      return "cuboid_precise";
    }

    @Override
    public Component tooltip() {
      return Component.translatable("enhanced_commands.region.cuboid");
    }

    @Override
    public FunctionLikeParser.SequentialParams<? extends RegionArgument<? extends PreciseCuboidRegion>> parser() {
      return new PreciseCuboidRegion.Parser();
    }

    @Override
    public @NotNull MapCodec<PreciseCuboidRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionArgument<? extends PreciseCuboidRegion>> getArgumentCodec() {
      return PreciseCuboidRegionArgument.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.SequentialParams<PreciseCuboidRegionArgument> {
    private EnhancedPosArgument from;
    private EnhancedPosArgument to;

    @Override
    public PreciseCuboidRegionArgument getParseResult(ParseContext<?> parseContext) {
      return new PreciseCuboidRegionArgument(from, to);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = new EnhancedPosArgumentType(EnhancedPosArgumentType.NumberType.DOUBLE_ONLY, EnhancedPosArgumentType.IntAlignType.UNCHANGED);
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
      return (to != null || EnhancedPosArgument.isInt(from)) ? 1 : 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      // 如果接受到了以空格区分的参数，那么不需要接受第二个参数了。
      return to != null ? 1 : 2;
    }
  }
}
