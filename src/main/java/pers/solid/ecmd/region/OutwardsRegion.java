package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.NbtUtil;

import java.util.Iterator;
import java.util.function.Function;

public record OutwardsRegion(Vec3i center, int x, int y, int z) implements IntBackedRegion {
  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return this.minContainingBlockBox().contains(vec3i);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return BlockPos.iterateOutwards(new BlockPos(center), x, y, z).iterator();
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.OUTWARDS;
  }

  @Override
  public @NotNull OutwardsRegion rotated(@NotNull Vec3i pivot, @NotNull BlockRotation blockRotation) {
    if (blockRotation == BlockRotation.CLOCKWISE_90 || blockRotation == BlockRotation.COUNTERCLOCKWISE_90) {
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
  public @NotNull BlockBox minContainingBlockBox() {
    return BlockBox.create(center.add(-x, -y, -z), center.add(x, y, z));
  }

  @Override
  public @NotNull String asString() {
    return "outwards(%s %s %s, %s %s %s)".formatted(Integer.toString(center.getX()), Integer.toString(center.getY()), Integer.toString(center.getZ()), Integer.toString(x), Integer.toString(y), Integer.toString(z));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("center", NbtUtil.fromVec3i(center));
    nbtCompound.putInt("x", x);
    nbtCompound.putInt("y", y);
    nbtCompound.putInt("z", z);
  }

  public enum Type implements RegionType<OutwardsRegion> {
    INSTANCE;

    @Override
    public String functionName() {
      return "outwards";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.outwards");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return new Parser();
    }

    @Override
    public @NotNull OutwardsRegion fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new OutwardsRegion(
          NbtUtil.toVec3i(nbtCompound.getCompound("center")),
          nbtCompound.getInt("x"),
          nbtCompound.getInt("y"),
          nbtCompound.getInt("z")
      );
    }
  }

  public static final class Parser implements FunctionParamsParser<RegionArgument> {
    private PosArgument center;
    private int x, y, z;
    private int dimensionNumber = 0;

    @Override
    public RegionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException {
      final int paramY = dimensionNumber < 2 ? x : y;
      final int paramZ = dimensionNumber < 3 ? x : z;
      return source -> new OutwardsRegion(center.toAbsoluteBlockPos(source), x, paramY, paramZ);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        ArgumentType<PosArgument> argumentType = EnhancedPosArgumentType.blockPos();
        center = parser.parseAndSuggestArgument(argumentType);
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
