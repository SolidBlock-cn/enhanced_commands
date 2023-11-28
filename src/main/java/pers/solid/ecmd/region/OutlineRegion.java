package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record OutlineRegion(OutlineType outlineType, Region region) implements RegionBasedRegion<OutlineRegion, Region> {
  public static Region of(Region region, OutlineTypes outlineType) throws CommandSyntaxException {
    try {
      if (region instanceof BlockCuboidRegion cuboidRegion) {
        if (outlineType == OutlineTypes.FLOOR_AND_CEIL) {
          if (cuboidRegion.minY() == cuboidRegion.maxY() || cuboidRegion.minY() == cuboidRegion.maxY() + 1) {
            return cuboidRegion;
          } else {
            return new UnionRegion(List.of(new BlockCuboidRegion(cuboidRegion.minX(), cuboidRegion.minY(), cuboidRegion.minZ(), cuboidRegion.maxX(), cuboidRegion.minY(), cuboidRegion.maxZ()), new BlockCuboidRegion(cuboidRegion.minX(), cuboidRegion.maxY(), cuboidRegion.minZ(), cuboidRegion.maxX(), cuboidRegion.maxY(), cuboidRegion.maxZ())));
          }
        } else if (outlineType == OutlineTypes.WALL || outlineType == OutlineTypes.WALL_CONNECTED) {
          return new CuboidWallRegion(cuboidRegion, 1);
        } else {
          return new CuboidOutlineRegion(cuboidRegion, 1);
        }
      } else if (region instanceof CylinderRegion cylinderRegion) {
        return new HollowCylinderRegion(cylinderRegion, outlineType);
      } else {
        return new OutlineRegion(outlineType, region);
      }
    } catch (RuntimeException e) {
      if (e.getCause() instanceof CommandSyntaxException commandSyntaxException) {
        throw commandSyntaxException;
      } else {
        throw e;
      }
    }
  }

  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return false;
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return outlineType.modifiedTest(region::contains, new BlockPos(vec3i));
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return stream().iterator();
  }

  @Override
  public Stream<BlockPos> stream() {
    return region.stream().filter(this::contains);
  }

  @Override
  public @NotNull RegionType<OutlineRegion> getType() {
    return RegionTypes.OUTLINE;
  }

  @Override
  public double volume() {
    return region.volume();
  }

  @Override
  public long numberOfBlocksAffected() {
    return region.numberOfBlocksAffected();
  }

  @Override
  public @NotNull String asString() {
    return "outline(" + region.asString() + ", " + outlineType.asString() + ")";
  }

  @Override
  public @Nullable Box minContainingBox() {
    return region.minContainingBox();
  }

  @Override
  public OutlineRegion newRegion(Region region) {
    return new OutlineRegion(outlineType, region);
  }

  public interface OutlineType extends StringIdentifiable {
    boolean modifiedTest(Predicate<BlockPos> predicate, BlockPos blockPos);

    @Override
    default String asString() {
      return toString();
    }
  }

  public interface BasicOutlineType extends OutlineType {
    Stream<BlockPos> streamNearbyPos(BlockPos blockPos);

    @Override
    default boolean modifiedTest(Predicate<BlockPos> predicate, BlockPos blockPos) {
      return predicate.test(blockPos) && streamNearbyPos(blockPos).anyMatch(modifiedPos -> !predicate.test(modifiedPos));
    }
  }

  public enum OutlineTypes implements BasicOutlineType {
    /**
     * The pos itself is in the region, but one if its near pos is not in the region.
     */
    OUTLINE("outline") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return Direction.stream().map(blockPos::offset);
      }
    },
    OUTLINE_CONNECTED("outline_connected") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return BlockPos.stream(-1, -1, -1, 1, 1, 1).filter(blockPos1 -> blockPos1 != BlockPos.ORIGIN).map(blockPos::add);
      }
    },
    WALL("wall") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return Direction.Type.HORIZONTAL.stream().map(blockPos::offset);
      }
    },
    WALL_CONNECTED("wall_connected") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return BlockPos.stream(-1, 0, -1, 1, 0, 1).filter(blockPos1 -> blockPos1 != BlockPos.ORIGIN).map(blockPos::add);
      }
    },
    FLOOR_AND_CEIL("floor_and_ceil") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return Stream.of(blockPos.up(), blockPos.down());
      }
    };

    private final String name;
    public static final Codec<OutlineTypes> CODEC = StringIdentifiable.createCodec(OutlineTypes::values);

    OutlineTypes(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return name;
    }

    public MutableText getDisplayName() {
      return Text.translatable("enhanced_commands.outline_type." + name);
    }
  }

  public enum Type implements RegionType<OutlineRegion> {
    OUTLINE_TYPE;

    @Override
    public String functionName() {
      return "outline";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.outline");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return new Parser();
    }
  }

  public static final class Parser implements FunctionParamsParser<RegionArgument> {
    private OutlineType outlineType = OutlineTypes.OUTLINE;
    private RegionArgument regionArgument;

    @Override
    public RegionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new OutlineRegion(outlineType, regionArgument.toAbsoluteRegion(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 1) {
        outlineType = parser.parseAndSuggestEnums(OutlineTypes.values(), OutlineTypes::getDisplayName, OutlineTypes.CODEC);
      } else if (paramIndex == 0) {
        regionArgument = RegionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
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
