package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.GeoUtil;

import java.util.Iterator;
import java.util.function.Function;

public record OutwardsRegion(Vec3i center, int x, int y, int z) implements IntBackedRegion {
  public static final MapCodec<OutwardsRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3i.CODEC.fieldOf("center").forGetter(OutwardsRegion::center), Codec.INT.fieldOf("x").forGetter(OutwardsRegion::x), Codec.INT.fieldOf("y").forGetter(OutwardsRegion::y), Codec.INT.fieldOf("z").forGetter(OutwardsRegion::z)).apply(i, OutwardsRegion::new));

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return this.minContainingBlockBox().isInside(vec3i);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return BlockPos.withinManhattan(new BlockPos(center), x, y, z).iterator();
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.OUTWARDS;
  }

  @Override
  public @NotNull OutwardsRegion rotated(@NotNull Vec3i pivot, @NotNull Rotation blockRotation) {
    if (blockRotation == Rotation.CLOCKWISE_90 || blockRotation == Rotation.COUNTERCLOCKWISE_90) {
      return new OutwardsRegion(GeoUtil.rotate(center, blockRotation, pivot), z, y, x);
    } else {
      return new OutwardsRegion(GeoUtil.rotate(center, blockRotation, pivot), x, y, z);
    }
  }

  @Override
  public OutwardsRegion transformedInt(Function<Vec3i, Vec3i> transformation) {
    return new OutwardsRegion(transformation.apply(center), x, y, z);
  }

  @Override
  public long numberOfBlocksAffected() {
    return (2L * x + 1) * (2L * y + 1) * (2L * z + 1);
  }

  @Override
  public @NotNull BoundingBox minContainingBlockBox() {
    return BoundingBox.fromCorners(center.offset(-x, -y, -z), center.offset(x, y, z));
  }

  @Override
  public @NotNull String asString() {
    return "outwards(%s %s %s, %s %s %s)".formatted(Integer.toString(x), Integer.toString(y), Integer.toString(z), Integer.toString(center.getX()), Integer.toString(center.getY()), Integer.toString(center.getZ()));
  }

  public enum Type implements RegionType<OutwardsRegion> {
    INSTANCE;

    @Override
    public String functionName() {
      return "outwards";
    }

    @Override
    public Component tooltip() {
      return Component.translatable("enhanced_commands.region.outwards");
    }

    @Override
    public FunctionContentParser.SequentialParams<OutwardsRegionProvider> parser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<OutwardsRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionProvider<OutwardsRegion>> getArgumentCodec() {
      return OutwardsRegionProvider.CODEC;
    }
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<OutwardsRegionProvider> {
    private EnhancedCoordinates center = EnhancedPosArgument.CURRENT_BLOCK_POS_CENTER;
    private int x, y, z;
    private int dimensionNumber = 0;

    @Override
    public OutwardsRegionProvider getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      final int paramY = dimensionNumber < 2 ? x : y;
      final int paramZ = dimensionNumber < 3 ? x : z;
      return new OutwardsRegionProvider(center, x, paramY, paramZ);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 1) {
        ArgumentType<EnhancedCoordinates> argumentType = EnhancedPosArgument.blockPos();
        center = parseContext.parseAndSuggestArgument(argumentType);
      } else if (paramIndex == 0) {
        final StringReader reader = parseContext.reader();
        x = reader.readInt();
        dimensionNumber = 1;
        reader.skipWhitespace();
        if (Character.isWhitespace(reader.peek(-1)) && reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          y = reader.readInt();
          dimensionNumber = 2;
          reader.skipWhitespace();
          if (Character.isWhitespace(reader.peek(-1)) && reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
            z = reader.readInt();
            dimensionNumber = 3;
            reader.skipWhitespace();
          }
        }
      }
    }
  }
}
