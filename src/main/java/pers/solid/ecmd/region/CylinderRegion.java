package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

public record CylinderRegion(@Range(from = 0, to = Long.MAX_VALUE) double radius, @Range(from = 0, to = Long.MAX_VALUE) double height, Vec3d center) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    if (center.y + height / 2 <= vec3d.y || center.y - height / 2 > vec3d.y) {
      return false; // not in this height
    } else {
      // whether within this radius
      return Vector2d.distance(center.x, center.z, vec3d.x, vec3d.z) <= radius;
    }
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return stream().iterator();
  }

  public int getBottomHeight() {
    return MathHelper.ceil(center.y - height / 2 - 0.5);
  }

  public int getTopHeight() {
    // round(center.y + height/2) - 1, round down 0.5
    return MathHelper.ceil(center.y + height / 2 - 1.5);
  }

  @Override
  public Stream<BlockPos> stream() {
    final Stream<BlockPos> oneHeightRound = BlockPos.stream(MathHelper.ceil(center.x - radius - 0.5), 0, MathHelper.ceil(center.z - radius - 0.5), MathHelper.floor(center.x + radius - 0.5), 0, MathHelper.floor(center.z + radius - 0.5)) // a one-height cuboid that contains a round
        .filter(blockPos -> {
          final Vec3d centerPos = blockPos.toCenterPos();
          return Vector2d.distance(centerPos.x, centerPos.z, center.x, center.z) <= radius;
        });
    final int bottomHeight = getBottomHeight();
    final int topHeight = getTopHeight();
    if (bottomHeight > topHeight) {
      // 这种情况一般不应该发生
      return Stream.empty();
    }
    return oneHeightRound
        .flatMap(blockPos -> BlockPos.stream(blockPos.getX(), bottomHeight, blockPos.getZ(), blockPos.getX(), topHeight, blockPos.getZ()));
  }

  @Override
  public CylinderRegion transformed(Function<Vec3d, Vec3d> transformation) {
    return new CylinderRegion(radius, height, transformation.apply(center));
  }

  public static final SimpleCommandExceptionType MUST_EXPAND_VERTICALLY = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.region.exception.cylinder_must_expand_vertically"));

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
      return new CylinderRegion(radius, height + offset, center.add(Vec3d.of(direction.getVector()).multiply(offset / 2)));
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
  public @NotNull Region expanded(double offset, Direction.Type type) {
    if (type == Direction.Type.VERTICAL) {
      if (offset * 2 < -height) {
        throw new IllegalArgumentException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().create(-height / 2, offset));
      }
      return new CylinderRegion(radius, height + 2 * offset, center);
    } else {
      throw new UnsupportedOperationException(MUST_EXPAND_VERTICALLY.create());
    }
  }

  @Override
  public @NotNull RegionType<CylinderRegion> getType() {
    return RegionTypes.CYLINDER;
  }

  @Override
  public double volume() {
    return Math.PI * radius * radius * height;
  }

  @Override
  public @NotNull String asString() {
    return String.format("cyl(%s, %s, %s %s %s)", radius, height, center.x, center.y, center.z);
  }

  @Override
  public @NotNull Box minContainingBox() {
    return new Box(center.x - radius, center.y - height / 2, center.z - radius, center.x + radius, center.y + height / 2, center.z + radius);
  }

  public enum Type implements RegionType<CylinderRegion> {
    CYLINDER_TYPE;

    @Override
    public String functionName() {
      return "cyl";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.cylinder");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return new Parser();
    }
  }

  public static final class Parser implements FunctionParamsParser<RegionArgument> {
    private double radius;
    private double height = 1;
    private PosArgument center = EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER;

    @Override
    public RegionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new CylinderRegion(radius, height, center.toAbsolutePos(source));
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
        final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
        center = parser.parseAndSuggestArgument(type);
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 3;
    }
  }
}
