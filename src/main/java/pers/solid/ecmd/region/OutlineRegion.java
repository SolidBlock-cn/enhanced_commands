package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record OutlineRegion(OutlineType outlineType, Region region) implements Region {
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
  public @NotNull OutlineRegion moved(@NotNull Vec3d relativePos) {
    return new OutlineRegion(outlineType, region.moved(relativePos));
  }

  @Override
  public @NotNull OutlineRegion moved(@NotNull Vec3i relativePos) {
    return new OutlineRegion(outlineType, region.moved(relativePos));
  }

  @Override
  public @NotNull OutlineRegion rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return new OutlineRegion(outlineType, region.rotated(pivot, blockRotation));
  }

  @Override
  public @NotNull OutlineRegion mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return new OutlineRegion(outlineType, region.mirrored(pivot, axis));
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
    return "outline(" + region.asString() + ", " + outlineType + ")";
  }

  @Override
  public @Nullable Box maxContainingBox() {
    return region.maxContainingBox();
  }

  public interface OutlineType extends StringIdentifiable {
    boolean modifiedTest(Predicate<BlockPos> predicate, BlockPos blockPos);

    @Override
    default String asString() {
      return "<custom outline type: " + this + ">";
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
    EXPOSE("outline") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return Direction.stream().map(blockPos::offset);
      }
    },
    NEARLY_EXPOSE("outline_connected") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return BlockPos.stream(-1, -1, -1, 1, 1, 1).filter(blockPos1 -> blockPos1 != BlockPos.ORIGIN).map(blockPos::add);
      }
    },
    HORIZONTALLY_EXPOSE("wall") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return Direction.Type.HORIZONTAL.stream().map(blockPos::offset);
      }
    },
    HORIZONTALLY_NEARLY_EXPOSE("wall_connected") {
      @Override
      public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
        return BlockPos.stream(-1, 0, -1, 1, 0, 1).filter(blockPos1 -> blockPos1 != BlockPos.ORIGIN).map(blockPos::add);
      }
    },
    VERTICALLY_EXPOSE("floor_and_ceil") {
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
      return Text.translatable("enhancedCommands.outlineType." + name);
    }
  }

  public enum Type implements RegionType<OutlineRegion> {
    OUTLINE_TYPE;

    @Override
    public @Nullable RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<OutlineRegion>> {
    private OutlineType outlineType = OutlineTypes.EXPOSE;
    private RegionArgument<?> regionArgument;

    @Override
    public @NotNull String functionName() {
      return "outline";
    }

    @Override
    public Text tooltip() {
      return null;
    }

    @Override
    public RegionArgument<OutlineRegion> getParseResult(SuggestedParser parser) {
      return source -> new OutlineRegion(outlineType, regionArgument.toAbsoluteRegion(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 1) {
        outlineType = parser.readAndSuggestEnums(OutlineTypes.values(), OutlineTypes::getDisplayName, OutlineTypes.CODEC);
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
