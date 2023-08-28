package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Iterator;
import java.util.stream.Stream;

public record CylinderRegion(double radius, double height, Vec3d center) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    if (center.y + height / 2 < vec3d.y || center.y - height / 2 > vec3d.y) {
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
    // round(center.y + height/2) - 1
    return MathHelper.floor(center.y + height / 2 - 0.5);
  }

  @Override
  public Stream<BlockPos> stream() {
    return BlockPos.stream(MathHelper.ceil(center.x - radius - 0.5), 0, MathHelper.ceil(center.z - radius - 0.5), MathHelper.floor(center.x + radius - 0.5), 0, MathHelper.floor(center.z + radius - 0.5)) // a one-height cuboid that contains a round
        .filter(blockPos -> {
          final Vec3d centerPos = blockPos.toCenterPos();
          return Vector2d.distance(centerPos.x, centerPos.z, center.x, center.z) <= radius;
        }) // a one-height round
        .flatMap(blockPos -> BlockPos.stream(blockPos.getX(), getBottomHeight(), blockPos.getZ(), blockPos.getX(), getTopHeight(), blockPos.getZ()));
  }

  @Override
  public @NotNull CylinderRegion moved(@NotNull Vec3d relativePos) {
    return new CylinderRegion(radius, height, center.add(relativePos));
  }

  @Override
  public @NotNull CylinderRegion expanded(double offset, Direction direction) {
    if (direction.getAxis().isVertical()) {
      return new CylinderRegion(radius, height + offset, center.add(Vec3d.of(direction.getVector()).multiply(offset / 2)));
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public @NotNull CylinderRegion expanded(double offset, Direction.Axis axis) {
    if (axis.isVertical()) {
      if (offset * 2 > height) {
        throw new IllegalArgumentException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooHigh().create(height / 2, offset));
      }
      return new CylinderRegion(radius, height + 2 * offset, center);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public @NotNull CylinderRegion rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    return null;
  }

  @Override
  public @NotNull CylinderRegion mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    return null;
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

  public enum Type implements RegionType<CylinderRegion> {
    CYLINDER_TYPE;

    @Override
    public @Nullable RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<CylinderRegion>> {
    private double radius;
    private double height;
    private PosArgument center = EnhancedPosArgumentType.HERE_INT;

    @Override
    public @NotNull String functionName() {
      return "cyl";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.cylinder");
    }

    @Override
    public RegionArgument<CylinderRegion> getParseResult(SuggestedParser parser) throws CommandSyntaxException {
      return source -> new CylinderRegion(radius, height, center.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        radius = parser.reader.readDouble();
      } else if (paramIndex == 1) {
        height = parser.reader.readDouble();
      } else if (paramIndex == 2) {
        final EnhancedPosArgumentType type = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false);
        center = SuggestionUtil.suggestParserFromType(type, parser, suggestionsOnly);
      }
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 3;
    }
  }
}
