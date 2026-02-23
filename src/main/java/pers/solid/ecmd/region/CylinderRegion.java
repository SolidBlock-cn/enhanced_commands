package pers.solid.ecmd.region;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.StringUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public record CylinderRegion(@Range(from = 0, to = Long.MAX_VALUE) double radius, @Range(from = 0, to = Long.MAX_VALUE) double height, Vec3 center) implements Region {
  public static final SimpleCommandExceptionType MUST_EXPAND_VERTICALLY = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.region.exception.cylinder_must_expand_vertically"));
  public static final MapCodec<CylinderRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("radius").forGetter(CylinderRegion::radius), Codec.DOUBLE.fieldOf("height").forGetter(CylinderRegion::height), Vec3.CODEC.fieldOf("center").forGetter(CylinderRegion::center)).apply(i, CylinderRegion::new));

  @Override
  public boolean contains(@NotNull Vec3 vec3d) {
    if (center.y + height / 2 <= vec3d.y || center.y - height / 2 > vec3d.y) {
      return false; // not in this height
    } else {
      // whether within this radius
      return Vector2d.distance(center.x, center.z, vec3d.x, vec3d.z) <= radius;
    }
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    final Iterable<@NotNull BlockPos> oneHeightRound = Iterables.filter(BlockPos.betweenClosed(Mth.ceil(center.x - radius - 0.5), 0, Mth.ceil(center.z - radius - 0.5), Mth.floor(center.x + radius - 0.5), 0, Mth.floor(center.z + radius - 0.5)), blockPos -> {
      final Vec3 centerPos = blockPos.getCenter();
      return Vector2d.distance(centerPos.x, centerPos.z, center.x, center.z) <= radius;
    });// a one-height cuboid that contains a round
    final int bottomHeight = getBottomHeight();
    final int topHeight = getTopHeight();
    if (bottomHeight > topHeight) {
      // 这种情况一般不应该发生
      return Collections.emptyIterator();
    }
    return Iterables.concat(Iterables.transform(oneHeightRound, blockPos -> BlockPos.betweenClosed(blockPos.getX(), bottomHeight, blockPos.getZ(), blockPos.getX(), topHeight, blockPos.getZ()))).iterator();
  }

  public int getBottomHeight() {
    return Mth.ceil(center.y - height / 2 - 0.5);
  }

  public int getTopHeight() {
    // round(center.y + height/2) - 1, round down 0.5
    return Mth.ceil(center.y + height / 2 - 1.5);
  }

  @Override
  public @NotNull CylinderRegion transformed(Function<Vec3, Vec3> transformation) {
    return new CylinderRegion(radius, height, transformation.apply(center));
  }

  @Override
  public @NotNull Region expanded(double offset) {
    throw new UnsupportedOperationException(MUST_EXPAND_VERTICALLY.create());
  }

  @Override
  public @NotNull CylinderRegion expanded(double offset, Direction direction) {
    if (direction.getAxis().isVertical()) {
      if (offset < -height) {
        throw new IllegalArgumentException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().create(-height, offset));
      }
      return new CylinderRegion(radius, height + offset, center.add(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(offset / 2)));
    } else {
      throw new UnsupportedOperationException(MUST_EXPAND_VERTICALLY.create());
    }
  }

  @Override
  public @NotNull CylinderRegion expanded(double offset, Direction.Axis axis) {
    if (axis.isVertical()) {
      if (offset * 2 < -height) {
        throw new IllegalArgumentException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().create(-height / 2, offset));
      }
      return new CylinderRegion(radius, height + 2 * offset, center);
    } else {
      throw new UnsupportedOperationException(MUST_EXPAND_VERTICALLY.create());
    }
  }

  @Override
  public @NotNull Region expanded(double offset, Direction.Plane type) {
    if (type == Direction.Plane.VERTICAL) {
      if (offset * 2 < -height) {
        throw new IllegalArgumentException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().create(-height / 2, offset));
      }
      return new CylinderRegion(radius, height + 2 * offset, center);
    } else {
      throw new UnsupportedOperationException(MUST_EXPAND_VERTICALLY.create());
    }
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.CYLINDER;
  }

  @Override
  public double volume() {
    return Math.PI * radius * radius * height;
  }

  @Override
  public @NotNull String asString() {
    return String.format("cyl(%s, %s, %s)", StringUtil.nf.format(radius), StringUtil.nf.format(height), StringUtil.wrapVector(center));
  }

  @Override
  public @NotNull AABB minContainingBox() {
    return new AABB(center.x - radius, center.y - height / 2, center.z - radius, center.x + radius, center.y + height / 2, center.z + radius);
  }

  public enum Type implements RegionType<CylinderRegion> {
    CYLINDER_TYPE;

    @Override
    public String functionName() {
      return "cyl";
    }

    @Override
    public Component tooltip() {
      return Component.translatable("enhanced_commands.region.cylinder");
    }

    @Override
    public Parser parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<CylinderRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionArgument<CylinderRegion>> getArgumentCodec() {
      return CylinderRegionArgument.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.MixedParams<CylinderRegionArgument> {
    private @Nullable Double radius = null;
    private @Nullable Double height = null;
    private @Nullable EnhancedPosArgument center = null;

    @Override
    public CylinderRegionArgument getParseResult(ParseContext<?> parseContext) {
      return new CylinderRegionArgument(radius == null ? 1 : radius, height == null ? 1 : height, center == null ? EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER : center);
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
        final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
        center = parseContext.parseAndSuggestArgument(type);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return radius == null ? 1 : 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 3;
    }

    private static final Set<String> SUPPORTED_PARAMS = Set.of("radius", "height", "center");

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
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      switch (paramName) {
        case "radius" -> parseSequentialParameter(parseContext, 0);
        case "height" -> parseSequentialParameter(parseContext, 1);
        case "center" -> parseSequentialParameter(parseContext, 2);
      }
    }
  }
}
