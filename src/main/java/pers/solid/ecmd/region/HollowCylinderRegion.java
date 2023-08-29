package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record HollowCylinderRegion(CylinderRegion cylinderRegion, OutlineRegion.OutlineTypes outlineType) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return contains(BlockPos.ofFloored(vec3d));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    final Vec3d center = cylinderRegion.center();
    final double radius = cylinderRegion.radius();
    final long topHeight = cylinderRegion.getTopHeight();
    final long bottomHeight = cylinderRegion.getBottomHeight();
    if (outlineType == OutlineRegion.OutlineTypes.EXPOSE || outlineType == OutlineRegion.OutlineTypes.NEARLY_EXPOSE || outlineType == OutlineRegion.OutlineTypes.VERTICALLY_EXPOSE) {
      // match the top or bottom ceiling
      if (vec3i.getY() == bottomHeight || vec3i.getY() == topHeight) {
        return horizontallyWithinCylinder(cylinderRegion, Vec3d.ofCenter(vec3i));
      }
    }
    if (outlineType != OutlineRegion.OutlineTypes.VERTICALLY_EXPOSE) {
      // match the walls
      if (vec3i.getY() >= bottomHeight && vec3i.getY() <= topHeight) {
        return horizontallyWithinHollowCylinder(cylinderRegion, outlineType, new BlockPos(vec3i));
      }
    }
    return false;
  }

  public static boolean horizontallyWithinCylinder(CylinderRegion cylinderRegion, Vec3d vec3d) {
    return Vector2d.distance(vec3d.x, vec3d.z, cylinderRegion.center().x, cylinderRegion.center().z) <= cylinderRegion.radius();
  }

  public static boolean horizontallyWithinHollowCylinder(CylinderRegion cylinderRegion, OutlineRegion.OutlineTypes outlineType, BlockPos testPos) {
    outlineType = switch (outlineType) {
      case EXPOSE ->
          OutlineRegion.OutlineTypes.HORIZONTALLY_EXPOSE;
      case NEARLY_EXPOSE ->
          OutlineRegion.OutlineTypes.HORIZONTALLY_NEARLY_EXPOSE;
      default ->
          outlineType;
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
    final Vec3d center = cylinderRegion.center();
    final double radius = cylinderRegion.radius();
    final int topHeight = cylinderRegion.getTopHeight();
    final int bottomHeight = cylinderRegion.getBottomHeight();

    final Iterable<BlockPos> iterable = BlockPos.iterate(MathHelper.ceil(center.x - radius - 0.5), 0, MathHelper.ceil(center.z - radius - 0.5), MathHelper.floor(center.x + radius - 0.5), 0, MathHelper.floor(center.z + radius - 0.5));
    final Stream<BlockPos> flatOutlineRoundStream = Streams.stream(iterable).filter(blockPos -> horizontallyWithinHollowCylinder(cylinderRegion, outlineType, blockPos));
    if (outlineType == OutlineRegion.OutlineTypes.EXPOSE || outlineType == OutlineRegion.OutlineTypes.NEARLY_EXPOSE || outlineType == OutlineRegion.OutlineTypes.VERTICALLY_EXPOSE) {
      if (topHeight == bottomHeight) {
        return Streams.stream(iterable).map(blockPos -> blockPos.withY(bottomHeight));
      }
      List<Stream<BlockPos>> parts = new ArrayList<>();
      // add top and bottom ceiling
      parts.add(Streams.stream(iterable).filter(blockPos -> horizontallyWithinCylinder(cylinderRegion, Vec3d.ofCenter(blockPos))).flatMap(blockPos -> Stream.of(blockPos.withY(topHeight), blockPos.withY(bottomHeight))));
      // add walls that excluded the top and bottom ceiling
      if (outlineType != OutlineRegion.OutlineTypes.VERTICALLY_EXPOSE) {
        parts.add(flatOutlineRoundStream.flatMap(blockPos -> BlockPos.stream(blockPos.getX(), bottomHeight + 1, blockPos.getZ(), blockPos.getX(), topHeight - 1, blockPos.getZ()).map(BlockPos::toImmutable)));
      }
      return parts.stream().flatMap(Function.identity());
    } else {
      // walls only
      return flatOutlineRoundStream.flatMap(blockPos -> BlockPos.stream(blockPos.getX(), bottomHeight, blockPos.getZ(), blockPos.getX(), topHeight, blockPos.getZ()));
    }
  }

  @Override
  public @NotNull HollowCylinderRegion moved(@NotNull Vec3d relativePos) {
    return new HollowCylinderRegion(cylinderRegion.moved(relativePos), outlineType);
  }

  @Override
  public @NotNull HollowCylinderRegion rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    return new HollowCylinderRegion(cylinderRegion.rotated(center, blockRotation), outlineType);
  }

  @Override
  public @NotNull HollowCylinderRegion mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    return new HollowCylinderRegion(cylinderRegion.mirrored(center, axis), outlineType);
  }

  @Override
  public @NotNull HollowCylinderRegion expanded(double offset, Direction.Axis axis) {
    return new HollowCylinderRegion(cylinderRegion.expanded(offset, axis), outlineType);
  }

  @Override
  public @NotNull HollowCylinderRegion expanded(double offset, Direction direction) {
    return new HollowCylinderRegion(cylinderRegion.expanded(offset, direction), outlineType);
  }

  @Override
  public @NotNull RegionType<HollowCylinderRegion> getType() {
    return RegionTypes.HOLLOW_CYLINDER;
  }

  @Override
  public double volume() {
    var roundSurface = Math.PI * MathHelper.square(cylinderRegion.radius());
    var roundWallSurface = roundSurface - Math.PI * MathHelper.square(cylinderRegion.radius() - 1);
    return switch (outlineType) {
      case EXPOSE, NEARLY_EXPOSE ->
          2 * roundSurface + (cylinderRegion.height() - 2) * roundWallSurface;
      case HORIZONTALLY_EXPOSE, HORIZONTALLY_NEARLY_EXPOSE ->
          cylinderRegion.height() * roundWallSurface;
      case VERTICALLY_EXPOSE ->
          2 * roundSurface;
    };
  }

  @Override
  public @NotNull String asString() {
    return String.format("hcyl(%s, %s, %s %s %s, %s)", cylinderRegion.radius(), cylinderRegion.height(), cylinderRegion.center().x, cylinderRegion.center().y, cylinderRegion.center().z, outlineType.asString());
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
    private double height;
    private PosArgument center = EnhancedPosArgumentType.HERE_INT;
    private OutlineRegion.OutlineTypes type = OutlineRegion.OutlineTypes.HORIZONTALLY_EXPOSE;

    @Override
    public @NotNull String functionName() {
      return "hcyl";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.hollow_cylinder");
    }

    @Override
    public RegionArgument<HollowCylinderRegion> getParseResult(SuggestedParser parser) {
      return source -> new HollowCylinderRegion(new CylinderRegion(radius, height, center.toAbsolutePos(source)), type);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        radius = parser.reader.readDouble();
      } else if (paramIndex == 1) {
        height = parser.reader.readDouble();
      } else if (paramIndex == 2) {
        center = SuggestionUtil.suggestParserFromType(new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false), parser, suggestionsOnly);
      } else if (paramIndex == 3) {
        type = parser.readAndSuggestEnums(OutlineRegion.OutlineTypes.values(), OutlineRegion.OutlineTypes::getDisplayName, OutlineRegion.OutlineTypes.CODEC);
      }
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 4;
    }
  }
}
