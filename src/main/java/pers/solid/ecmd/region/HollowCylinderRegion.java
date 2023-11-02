package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record HollowCylinderRegion(CylinderRegion region, OutlineRegion.OutlineTypes outlineType) implements RegionBasedRegion<HollowCylinderRegion, CylinderRegion> {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return contains(BlockPos.ofFloored(vec3d));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    final long topHeight = region.getTopHeight();
    final long bottomHeight = region.getBottomHeight();
    if (outlineType == OutlineRegion.OutlineTypes.OUTLINE || outlineType == OutlineRegion.OutlineTypes.OUTLINE_CONNECTED || outlineType == OutlineRegion.OutlineTypes.FLOOR_AND_CEIL) {
      // match the top or bottom ceiling
      if (vec3i.getY() == bottomHeight || vec3i.getY() == topHeight) {
        return horizontallyWithinCylinder(region, Vec3d.ofCenter(vec3i));
      }
    }
    if (outlineType != OutlineRegion.OutlineTypes.FLOOR_AND_CEIL) {
      // match the walls
      if (vec3i.getY() >= bottomHeight && vec3i.getY() <= topHeight) {
        return horizontallyWithinHollowCylinder(region, outlineType, new BlockPos(vec3i));
      }
    }
    return false;
  }

  public static boolean horizontallyWithinCylinder(CylinderRegion cylinderRegion, Vec3d vec3d) {
    return Vector2d.distance(vec3d.x, vec3d.z, cylinderRegion.center().x, cylinderRegion.center().z) <= cylinderRegion.radius();
  }

  public static boolean horizontallyWithinHollowCylinder(CylinderRegion cylinderRegion, OutlineRegion.OutlineTypes outlineType, BlockPos testPos) {
    outlineType = switch (outlineType) {
      case OUTLINE -> OutlineRegion.OutlineTypes.WALL;
      case OUTLINE_CONNECTED -> OutlineRegion.OutlineTypes.WALL_CONNECTED;
      default -> outlineType;
    };
    return outlineType.modifiedTest(blockPos -> {
      final Vec3d centerPos = blockPos.toCenterPos();
      return Vector2d.distance(centerPos.x, centerPos.z, cylinderRegion.center().x, cylinderRegion.center().z) <= cylinderRegion.radius();
    }, testPos);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return stream().iterator();
  }

  @Override
  public Stream<BlockPos> stream() {
    final Vec3d center = region.center();
    final double radius = region.radius();
    final int topHeight = region.getTopHeight();
    final int bottomHeight = region.getBottomHeight();

    final Iterable<BlockPos> iterable = BlockPos.iterate(MathHelper.ceil(center.x - radius - 0.5), 0, MathHelper.ceil(center.z - radius - 0.5), MathHelper.floor(center.x + radius - 0.5), 0, MathHelper.floor(center.z + radius - 0.5));
    final Stream<BlockPos> flatOutlineRoundStream = Streams.stream(iterable).filter(blockPos -> horizontallyWithinHollowCylinder(region, outlineType, blockPos));
    if (outlineType == OutlineRegion.OutlineTypes.OUTLINE || outlineType == OutlineRegion.OutlineTypes.OUTLINE_CONNECTED || outlineType == OutlineRegion.OutlineTypes.FLOOR_AND_CEIL) {
      if (topHeight == bottomHeight) {
        return Streams.stream(iterable).map(blockPos -> blockPos.withY(bottomHeight));
      } else if (topHeight < bottomHeight) {
        throw new IllegalStateException("Invalid hollow cylinder! topHeight < bottomHeight, topHeight = " + topHeight + ", bottomHeight = " + bottomHeight);
      }
      List<Stream<BlockPos>> parts = new ArrayList<>();
      // add top and bottom ceiling
      parts.add(Streams.stream(iterable).filter(blockPos -> horizontallyWithinCylinder(region, Vec3d.ofCenter(blockPos))).flatMap(blockPos -> Stream.of(blockPos.withY(topHeight), blockPos.withY(bottomHeight))));
      // add walls that excluded the top and bottom ceiling
      if (outlineType != OutlineRegion.OutlineTypes.FLOOR_AND_CEIL && topHeight - 1 > bottomHeight + 1) {
        parts.add(flatOutlineRoundStream.flatMap(blockPos -> BlockPos.stream(blockPos.getX(), bottomHeight + 1, blockPos.getZ(), blockPos.getX(), topHeight - 1, blockPos.getZ()).map(BlockPos::toImmutable)));
      }
      return parts.stream().flatMap(Function.identity());
    } else {
      // walls only
      return flatOutlineRoundStream.flatMap(blockPos -> BlockPos.stream(blockPos.getX(), bottomHeight, blockPos.getZ(), blockPos.getX(), topHeight, blockPos.getZ()));
    }
  }

  @Override
  public HollowCylinderRegion newRegion(CylinderRegion region) {
    return new HollowCylinderRegion(region, outlineType);
  }

  @Override
  public @NotNull RegionType<HollowCylinderRegion> getType() {
    return RegionTypes.HOLLOW_CYLINDER;
  }

  @Override
  public double volume() {
    var roundSurface = Math.PI * MathHelper.square(region.radius());
    var roundWallSurface = roundSurface - Math.PI * MathHelper.square(region.radius() - 1);
    return switch (outlineType) {
      case OUTLINE, OUTLINE_CONNECTED -> 2 * roundSurface + (region.height() - 2) * roundWallSurface;
      case WALL, WALL_CONNECTED -> region.height() * roundWallSurface;
      case FLOOR_AND_CEIL -> 2 * roundSurface;
    };
  }

  @Override
  public @NotNull String asString() {
    return String.format("hcyl(%s, %s, %s %s %s, %s)", region.radius(), region.height(), region.center().x, region.center().y, region.center().z, outlineType.asString());
  }

  @Override
  public @NotNull Box minContainingBox() {
    return region.minContainingBox();
  }

  public enum Type implements RegionType<HollowCylinderRegion> {
    HOLLOW_CYLINDER_TYPE;

    @Override
    public @Nullable RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<HollowCylinderRegion>> {
    private double radius;
    private double height = 1;
    private PosArgument center = EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER;
    private OutlineRegion.OutlineTypes type = OutlineRegion.OutlineTypes.WALL;

    @Override
    public @NotNull String functionName() {
      return "hcyl";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.hollow_cylinder");
    }

    @Override
    public RegionArgument<HollowCylinderRegion> getParseResult(SuggestedParser parser) {
      return source -> new HollowCylinderRegion(new CylinderRegion(radius, height, center.toAbsolutePos(source)), type);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        final int cursorBeforeReadDouble = parser.reader.getCursor();
        radius = parser.reader.readDouble();
        if (radius < 0) {
          parser.reader.setCursor(cursorBeforeReadDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(parser.reader, 0, radius);
        }
      } else if (paramIndex == 1) {
        final int cursorBeforeReadDouble = parser.reader.getCursor();
        height = parser.reader.readDouble();
        if (height < 0) {
          parser.reader.setCursor(cursorBeforeReadDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(parser.reader, 0, height);
        }
      } else if (paramIndex == 2) {
        center = ParsingUtil.suggestParserFromType(EnhancedPosArgumentType.posPreferringCenteredInt(), parser, suggestionsOnly);
      } else if (paramIndex == 3) {
        type = parser.readAndSuggestEnums(OutlineRegion.OutlineTypes.values(), OutlineRegion.OutlineTypes::getDisplayName, OutlineRegion.OutlineTypes.CODEC);
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 4;
    }
  }
}
