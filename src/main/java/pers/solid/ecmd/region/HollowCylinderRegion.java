package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.enums.OutlineType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public record HollowCylinderRegion(@NotNull OutlineType outlineType, @NotNull CylinderRegion region) implements RegionBasedRegion<HollowCylinderRegion, CylinderRegion> {
  public static final MapCodec<HollowCylinderRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          OutlineType.OUTLINE_TYPE_FIELD.forGetter(HollowCylinderRegion::outlineType),
          CylinderRegion.CODEC.fieldOf("region").forGetter(HollowCylinderRegion::region))
      .apply(i, HollowCylinderRegion::new));

  public static boolean horizontallyWithinCylinder(CylinderRegion cylinderRegion, Vec3d vec3d) {
    return Vector2d.distance(vec3d.x, vec3d.z, cylinderRegion.center().x, cylinderRegion.center().z) <= cylinderRegion.radius();
  }

  public static boolean horizontallyWithinHollowCylinder(CylinderRegion cylinderRegion, OutlineType outlineType, BlockPos testPos) {
    outlineType = switch (outlineType) {
      case OUTLINE -> OutlineType.WALL;
      case OUTLINE_CONNECTED -> OutlineType.WALL_CONNECTED;
      default -> outlineType;
    };
    return outlineType.modifiedTest(blockPos -> {
      final Vec3d centerPos = blockPos.toCenterPos();
      return Vector2d.distance(centerPos.x, centerPos.z, cylinderRegion.center().x, cylinderRegion.center().z) <= cylinderRegion.radius();
    }, testPos);
  }

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return contains(BlockPos.ofFloored(vec3d));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    final long topHeight = region.getTopHeight();
    final long bottomHeight = region.getBottomHeight();
    if (outlineType == OutlineType.OUTLINE || outlineType == OutlineType.OUTLINE_CONNECTED || outlineType == OutlineType.FLOOR_AND_CEIL) {
      // match the top or bottom ceiling
      if (vec3i.getY() == bottomHeight || vec3i.getY() == topHeight) {
        return horizontallyWithinCylinder(region, Vec3d.ofCenter(vec3i));
      }
    }
    if (outlineType != OutlineType.FLOOR_AND_CEIL) {
      // match the walls
      if (vec3i.getY() >= bottomHeight && vec3i.getY() <= topHeight) {
        return horizontallyWithinHollowCylinder(region, outlineType, new BlockPos(vec3i));
      }
    }
    return false;
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return stream().iterator();
  }

  @Override
  public Stream<@NotNull BlockPos> stream() {
    final Vec3d center = region.center();
    final double radius = region.radius();
    final int topHeight = region.getTopHeight();
    final int bottomHeight = region.getBottomHeight();

    final Iterable<@NotNull BlockPos> iterable = BlockPos.iterate(MathHelper.ceil(center.x - radius - 0.5), 0, MathHelper.ceil(center.z - radius - 0.5), MathHelper.floor(center.x + radius - 0.5), 0, MathHelper.floor(center.z + radius - 0.5));
    final Stream<@NotNull BlockPos> flatOutlineRoundStream = Streams.stream(iterable).filter(blockPos -> horizontallyWithinHollowCylinder(region, outlineType, blockPos));
    if (outlineType == OutlineType.OUTLINE || outlineType == OutlineType.OUTLINE_CONNECTED || outlineType == OutlineType.FLOOR_AND_CEIL) {
      if (topHeight == bottomHeight) {
        return Streams.stream(iterable).map(blockPos -> blockPos.withY(bottomHeight));
      } else if (topHeight < bottomHeight) {
        throw new IllegalStateException("Invalid hollow cylinder! topHeight < bottomHeight, topHeight = " + topHeight + ", bottomHeight = " + bottomHeight);
      }
      List<Stream<BlockPos>> parts = new ArrayList<>();
      // add top and bottom ceiling
      parts.add(Streams.stream(iterable).filter(blockPos -> horizontallyWithinCylinder(region, Vec3d.ofCenter(blockPos))).flatMap(blockPos -> Stream.of(blockPos.withY(topHeight), blockPos.withY(bottomHeight))));
      // add walls that excluded the top and bottom ceiling
      if (outlineType != OutlineType.FLOOR_AND_CEIL && topHeight - 1 > bottomHeight + 1) {
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
    return new HollowCylinderRegion(outlineType, region);
  }

  @Override
  public @NotNull Type getType() {
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
    return String.format("hcyl(%s, %s, %s, %s)", StringUtil.nf.format(region.radius()), StringUtil.nf.format(region.height()), StringUtil.wrapVector(region.center()), outlineType.asString());
  }

  @Override
  public @NotNull Box minContainingBox() {
    return region.minContainingBox();
  }

  public enum Type implements RegionType<HollowCylinderRegion> {
    HOLLOW_CYLINDER_TYPE;

    @Override
    public String functionName() {
      return "hcyl";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.hollow_cylinder");
    }

    @Override
    public Parser parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<HollowCylinderRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionArgument<HollowCylinderRegion>> getArgumentCodec() {
      return HollowCylinderRegionArgument.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.MixedParams<HollowCylinderRegionArgument> {
    private @Nullable Double radius = null;
    private @Nullable Double height = null;
    private @Nullable EnhancedPosArgument center = null;
    private OutlineType type = null;

    @Override
    public HollowCylinderRegionArgument getParseResult(ParseContext<?> parseContext) {
      return new HollowCylinderRegionArgument(type == null ? OutlineType.WALL : type, new CylinderRegionArgument(radius == null ? 1 : radius, height == null ? 1 : 0, center == null ? EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER : center));
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        if (radius != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "radius");
        }
        final int cursorBeforeReadDouble = reader.getCursor();
        radius = reader.readDouble();
        if (radius < 0) {
          reader.setCursor(cursorBeforeReadDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, radius);
        }
      } else if (paramIndex == 1) {
        if (height != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "height");
        }
        final int cursorBeforeReadDouble = reader.getCursor();
        height = reader.readDouble();
        if (height < 0) {
          reader.setCursor(cursorBeforeReadDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, height);
        }
      } else if (paramIndex == 2) {
        if (center != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "center");
        }
        ArgumentType<EnhancedPosArgument> argumentType = EnhancedPosArgumentType.posPreferringCenteredInt();
        center = parseContext.parseAndSuggestArgument(argumentType);
      } else if (paramIndex == 3) {
        if (type != null) {
          throw ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "type");
        }
        type = parseContext.parseAndSuggestEnums(OutlineType.values(), OutlineType::getDisplayName, OutlineType.CODEC);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return radius == null ? 1 : 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 4;
    }

    private static final Set<String> SUPPORTED_PARAMS = Set.of("radius", "height", "center", "type");

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return switch (paramName) {
        case "radius" -> radius != null;
        case "height" -> height != null;
        case "center" -> center != null;
        case "type" -> type != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      switch (paramName) {
        case "radius" -> parseSequentialParameter(parseContext, 0);
        case "height" -> parseSequentialParameter(parseContext, 1);
        case "center" -> parseSequentialParameter(parseContext, 2);
        case "type" -> parseSequentialParameter(parseContext, 3);
      }
    }
  }
}
