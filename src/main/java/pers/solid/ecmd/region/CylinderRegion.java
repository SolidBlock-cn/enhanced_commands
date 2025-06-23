package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

public record CylinderRegion(@Range(from = 0, to = Long.MAX_VALUE) double radius, @Range(from = 0, to = Long.MAX_VALUE) double height, Vec3d center) implements Region {
  public static final SimpleCommandExceptionType MUST_EXPAND_VERTICALLY = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.region.exception.cylinder_must_expand_vertically"));
  public static final MapCodec<CylinderRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("radius").forGetter(CylinderRegion::radius), Codec.DOUBLE.fieldOf("height").forGetter(CylinderRegion::height), Vec3d.CODEC.fieldOf("center").forGetter(CylinderRegion::center)).apply(i, CylinderRegion::new));

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
  public @NotNull CylinderRegion transformed(Function<Vec3d, Vec3d> transformation) {
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
  public @NotNull Type getType() {
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
      return Text.translatable("enhanced_commands.region.cylinder");
    }

    @Override
    public FunctionParamsParser<CylinderRegionArgument> functionParamsParser() {
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

  public static final class Parser implements FunctionParamsParser<CylinderRegionArgument> {
    private double radius;
    private double height = 1;
    private EnhancedPosArgument center = EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER;

    @Override
    public CylinderRegionArgument getParseResult(ParseContext<?> parseContext) {
      return new CylinderRegionArgument(radius, height, center);
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        final int cursorBeforeReadDouble = reader.getCursor();
        radius = reader.readDouble();
        if (radius < 0) {
          reader.setCursor(cursorBeforeReadDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, radius);
        }
      } else if (paramIndex == 1) {
        final int cursorBeforeReadDouble = reader.getCursor();
        height = reader.readDouble();
        if (height < 0) {
          reader.setCursor(cursorBeforeReadDouble);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, height);
        }
      } else if (paramIndex == 2) {
        final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
        center = parseContext.parseAndSuggestArgument(type);
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
