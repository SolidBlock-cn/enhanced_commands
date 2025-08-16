package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.enums.OutlineType;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record OutlineRegion(OutlineType outlineType, Region region) implements RegionBasedRegion<OutlineRegion, Region> {
  public static final MapCodec<OutlineRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(OutlineType.OUTLINE_TYPE_FIELD.forGetter(OutlineRegion::outlineType), Region.CODEC.fieldOf("region").forGetter(OutlineRegion::region)).apply(i, OutlineRegion::new));

  public static Region of(Region region, OutlineType outlineType) throws CommandSyntaxException {
    try {
      if (region instanceof BlockCuboidRegion cuboidRegion) {
        if (outlineType == OutlineType.FLOOR_AND_CEIL) {
          if (cuboidRegion.minY() == cuboidRegion.maxY() || cuboidRegion.minY() == cuboidRegion.maxY() + 1) {
            return cuboidRegion;
          } else {
            return new UnionRegion(List.of(new BlockCuboidRegion(cuboidRegion.minX(), cuboidRegion.minY(), cuboidRegion.minZ(), cuboidRegion.maxX(), cuboidRegion.minY(), cuboidRegion.maxZ()), new BlockCuboidRegion(cuboidRegion.minX(), cuboidRegion.maxY(), cuboidRegion.minZ(), cuboidRegion.maxX(), cuboidRegion.maxY(), cuboidRegion.maxZ())));
          }
        } else if (outlineType == OutlineType.WALL || outlineType == OutlineType.WALL_CONNECTED) {
          return new CuboidWallRegion(cuboidRegion, 1);
        } else {
          return new CuboidOutlineRegion(cuboidRegion, 1);
        }
      } else if (region instanceof CylinderRegion cylinderRegion) {
        return new HollowCylinderRegion(outlineType, cylinderRegion);
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
  public Stream<@NotNull BlockPos> stream() {
    return region.stream().filter(this::contains);
  }

  @Override
  public @NotNull Type getType() {
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

  public enum Type implements RegionType<OutlineRegion> {
    OUTLINE_TYPE;

    @Override
    public String functionName() {
      return "outline";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.outline");
    }

    @Override
    public FunctionLikeParser.SequentialParams<? extends OutlineRegionArgument> parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<OutlineRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends OutlineRegionArgument> getArgumentCodec() {
      return OutlineRegionArgument.CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.SequentialParams<OutlineRegionArgument> {
    private OutlineType outlineType = OutlineType.OUTLINE;
    private RegionArgument<?> regionArgument;

    @Override
    public OutlineRegionArgument getParseResult(ParseContext<?> parseContext) {
      return new OutlineRegionArgument(outlineType, regionArgument);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 1) {
        outlineType = parseContext.parseAndSuggestEnums(OutlineType.values(), OutlineType::getDisplayName, OutlineType.CODEC);
      } else if (paramIndex == 0) {
        regionArgument = RegionArgument.parse(parseContext);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }
  }
}
