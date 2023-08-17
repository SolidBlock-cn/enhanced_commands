package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record CuboidOutlineRegion(BlockCuboidRegion blockCuboidRegion, int thickness) implements IntBackedRegion {
  public static final DynamicCommandExceptionType NON_POSITIVE_THICKNESS = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.cuboid_outline.non_positive_thickness", o));
  public static final Dynamic2CommandExceptionType TOO_THICK = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.argument.region.cuboid_outline.too_thick", a, b));

  public CuboidOutlineRegion {
    if (thickness <= 0) {
      throw new IllegalArgumentException(NON_POSITIVE_THICKNESS.create(thickness));
    }
    final int maxAcceptableThickness = getMaxAcceptableThickness(blockCuboidRegion);
    if (thickness > maxAcceptableThickness) {
      throw new IllegalArgumentException(TOO_THICK.create(maxAcceptableThickness, thickness));
    }
  }

  public static int getMaxAcceptableThickness(BlockCuboidRegion blockCuboidRegion) {
    return NumberUtils.min(Math.floorDiv(blockCuboidRegion.maxX() - blockCuboidRegion.minX() + 1, 2), Math.floorDiv(blockCuboidRegion.maxY() - blockCuboidRegion.minY() + 1, 2), Math.floorDiv(blockCuboidRegion.maxZ() - blockCuboidRegion.minZ() + 1, 2));
  }

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return blockCuboidRegion.contains(vec3d) && blockCuboidRegion.expanded(-thickness).contains(vec3d);
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return blockCuboidRegion.contains(vec3i) && blockCuboidRegion.expanded(-thickness).contains(vec3i);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.concat(Iterators.transform(decompose().iterator(), BlockCuboidRegion::iterator));
  }

  @Override
  public Stream<BlockPos> stream() {
    return decompose().stream().flatMap(Region::stream);
  }

  public @NotNull List<BlockCuboidRegion> decompose() {
    final List<BlockCuboidRegion> walls = new CuboidWallRegion(blockCuboidRegion, thickness).decompose();
    return List.of(
        // top part
        new BlockCuboidRegion(blockCuboidRegion.minX(), blockCuboidRegion.maxY() - thickness + 1, blockCuboidRegion.minZ(), blockCuboidRegion.maxX(), blockCuboidRegion.maxY(), blockCuboidRegion.maxZ()),
        walls.get(0),
        walls.get(1),
        walls.get(2),
        walls.get(3),
        new BlockCuboidRegion(blockCuboidRegion.minX(), blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.maxX(), blockCuboidRegion.minY() + thickness - 1, blockCuboidRegion.maxZ())
    );
  }

  @Override
  public @NotNull CuboidOutlineRegion moved(@NotNull Vec3i relativePos) {
    return new CuboidOutlineRegion(blockCuboidRegion.moved(relativePos), thickness);
  }

  @Override
  public @NotNull CuboidOutlineRegion rotated(@NotNull Vec3i center, @NotNull BlockRotation blockRotation) {
    return null;
  }

  @Override
  public @NotNull CuboidOutlineRegion mirrored(Vec3i center, Direction.@NotNull Axis axis) {
    return null;
  }

  @Override
  public @NotNull RegionType<CuboidOutlineRegion> getType() {
    return RegionTypes.CUBOID_OUTLINE;
  }

  @Override
  public long numberOfBlocksAffected() {
    return blockCuboidRegion.numberOfBlocksAffected() - blockCuboidRegion.expanded(-thickness).numberOfBlocksAffected();
  }

  @Override
  public @NotNull String asString() {
    return String.format("cuboid_outline(%s %s %s, %s %s %s, %s)", blockCuboidRegion.minX(), blockCuboidRegion.minY(), blockCuboidRegion.minZ(), blockCuboidRegion.maxX(), blockCuboidRegion.maxY(), blockCuboidRegion.maxZ(), thickness);
  }

  public enum Type implements RegionType<CuboidOutlineRegion> {
    CUBOID_OUTLINE_TYPE;

    @Override
    public @Nullable RegionArgument<CuboidOutlineRegion> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static abstract sealed class AbstractParser<T extends Region> implements FunctionLikeParser<RegionArgument<T>> permits Parser, CuboidWallRegion.Parser {
    protected PosArgument fromPos, toPos;
    protected int thickness = 1;


    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        fromPos = SuggestionUtil.suggestParserFromType(new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false), parser, suggestionsOnly);
      } else if (paramIndex == 1) {
        toPos = SuggestionUtil.suggestParserFromType(new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false), parser, suggestionsOnly);
      } else if (paramIndex == 2) {
        thickness = parser.reader.readInt();
      }
    }
  }

  public static final class Parser extends AbstractParser<CuboidOutlineRegion> {
    @Override
    public @NotNull String functionName() {
      return "cuboid_outline";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.cuboid_outline");
    }

    @Override
    public RegionArgument<CuboidOutlineRegion> getParseResult() {
      return source -> new CuboidOutlineRegion(new BlockCuboidRegion(fromPos.toAbsoluteBlockPos(source), toPos.toAbsoluteBlockPos(source)), thickness);
    }
  }
}
