package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Iterator;

public record OutwardsRegion(Vec3i vec3i, int x, int y, int z) implements IntBackedRegion {
  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return maxContainingBlockBox().contains(vec3i);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return BlockPos.iterateOutwards(new BlockPos(vec3i), x, y, z).iterator();
  }

  @Override
  public @NotNull OutwardsRegion moved(@NotNull Vec3i relativePos) {
    return new OutwardsRegion(vec3i.add(relativePos), x, y, z);
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.OUTWARDS;
  }

  @Override
  public @NotNull OutwardsRegion rotated(@NotNull Vec3i pivot, @NotNull BlockRotation blockRotation) {
    if (blockRotation == BlockRotation.CLOCKWISE_90 || blockRotation == BlockRotation.COUNTERCLOCKWISE_90) {
      return new OutwardsRegion(GeoUtil.rotate(vec3i, blockRotation, pivot), z, y, x);
    } else {
      return new OutwardsRegion(GeoUtil.rotate(vec3i, blockRotation, pivot), x, y, z);
    }
  }

  @Override
  public @NotNull OutwardsRegion mirrored(Vec3i pivot, Direction.@NotNull Axis axis) {
    return new OutwardsRegion(GeoUtil.mirror(vec3i, axis, pivot), x, y, z);
  }

  @Override
  public long numberOfBlocksAffected() {
    return (2L * x + 1) * (2L * y + 1) * (2L * z + 1);
  }

  @Override
  public @NotNull BlockBox maxContainingBlockBox() {
    return BlockBox.create(vec3i.add(-x, -y, -z), vec3i.add(x, y, z));
  }

  @Override
  public @NotNull String asString() {
    return "outwards(%s %s %s, %s %s %s)".formatted(Integer.toString(vec3i.getX()), Integer.toString(vec3i.getY()), Integer.toString(vec3i.getZ()), Integer.toString(x), Integer.toString(y), Integer.toString(z));
  }

  public enum Type implements RegionType<OutwardsRegion> {
    INSTANCE;

    @Override
    public @Nullable RegionArgument<OutwardsRegion> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static final class Parser implements FunctionLikeParser<RegionArgument<OutwardsRegion>> {
    private PosArgument center;
    private int x, y, z;
    private int dimensionNumber = 0;

    @Override
    public @NotNull String functionName() {
      return "outwards";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.region.outwards");
    }

    @Override
    public RegionArgument<OutwardsRegion> getParseResult(SuggestedParser parser) throws CommandSyntaxException {
      final int paramY = dimensionNumber < 2 ? x : y;
      final int paramZ = dimensionNumber < 3 ? x : z;
      return source -> new OutwardsRegion(center.toAbsoluteBlockPos(source), x, paramY, paramZ);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        center = ParsingUtil.suggestParserFromType(EnhancedPosArgumentType.blockPos(), parser, suggestionsOnly);
      } else if (paramIndex == 1) {
        final StringReader reader = parser.reader;
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
