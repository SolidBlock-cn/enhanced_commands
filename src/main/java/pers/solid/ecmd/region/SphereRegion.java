package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.NbtUtil;

import java.util.Iterator;
import java.util.function.Function;

public record SphereRegion(double radius, Vec3d center) implements Region {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return vec3d.isInRange(center, radius);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Streams.stream(new CuboidRegion(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))).filter(blockPos -> blockPos.isWithinDistance(center, radius)).iterator();
  }

  @Override
  public @NotNull SphereRegion transformed(Function<Vec3d, Vec3d> transformation) {
    return new SphereRegion(radius, transformation.apply(center));
  }

  @Override
  public @NotNull RegionType<?> getType() {
    return RegionTypes.SPHERE;
  }

  public static final SimpleCommandExceptionType EXPAND_FAILED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.region.exception.sphere_cannot_expand"));

  @Override
  public @NotNull SphereRegion expanded(double offset) {
    return new SphereRegion(radius + offset, center);
  }

  @Override
  public @NotNull SphereRegion expanded(double offset, Direction.Type type) {
    throw new UnsupportedOperationException(EXPAND_FAILED.create());
  }

  @Override
  public @NotNull SphereRegion expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException(EXPAND_FAILED.create());
  }

  @Override
  public @NotNull SphereRegion expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException(EXPAND_FAILED.create());
  }

  @Override
  public double volume() {
    return 4d / 3d * Math.PI * Math.pow(radius, 3);
  }

  @Override
  public @NotNull String asString() {
    return "sphere(%s, %s %s %s)".formatted(radius, center.x, center.y, center.z);
  }

  @Override
  public @NotNull Box minContainingBox() {
    return Box.of(center, 2 * radius, 2 * radius, 2 * radius);
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putDouble("radius", radius);
    nbtCompound.put("center", NbtUtil.fromVec3d(center));
  }

  public enum Type implements RegionType<SphereRegion> {
    SPHERE_TYPE;

    @Override
    public String functionName() {
      return "sphere";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.sphere");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return new Parser();
    }

    @Override
    public @NotNull SphereRegion fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new SphereRegion(
          nbtCompound.getDouble("radius"),
          NbtUtil.toVec3d(nbtCompound.getCompound("center"))
      );
    }
  }

  public static final class Parser implements FunctionParamsParser<RegionArgument> {
    private PosArgument centerPos = EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER;
    private double radius;

    @Override
    public RegionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new SphereRegion(radius, centerPos.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
      if (paramIndex == 0) {
        radius = parser.reader.readDouble();
      } else if (paramIndex == 1) {
        centerPos = parser.parseAndSuggestArgument(type);
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }
  }
}
