package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.EnumOrRandom;
import pers.solid.ecmd.util.FunctionParamsParser;

public record RotateBlockFunction(@NotNull EnumOrRandom<BlockRotation> rotation) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "rotate(" + rotation.asString() + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    return blockState.rotate(rotation.apply(world.getRandom()));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("rotation", rotation.asString());
  }

  @Override
  public @NotNull BlockFunctionType<RotateBlockFunction> getType() {
    return BlockFunctionTypes.ROTATE;
  }

  public enum Type implements BlockFunctionType<RotateBlockFunction> {
    ROTATE_TYPE;

    @Override
    public @NotNull RotateBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new RotateBlockFunction(EnumOrRandom.parse(BlockRotation.CODEC, nbtCompound.getString("rotation"), BlockRotation::values).orElseThrow());
    }
  }

  public static class Parser implements FunctionParamsParser<BlockFunctionArgument> {
    private EnumOrRandom<BlockRotation> rotation;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public RotateBlockFunction getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return new RotateBlockFunction(rotation);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      rotation = EnumOrRandom.parseAndSuggest(BlockRotation.values(), BlockRotation.CODEC, parser);
    }
  }
}
