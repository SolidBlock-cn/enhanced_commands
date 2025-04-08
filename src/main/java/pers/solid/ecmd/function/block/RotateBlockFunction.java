package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.EnumOrRandom;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

public record RotateBlockFunction(@NotNull EnumOrRandom<BlockRotation> rotation) implements BlockFunction {
  public static final MapCodec<RotateBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(RotateBlockFunction::new, EnumOrRandom.getCodec(BlockRotation.CODEC, BlockRotation::values).fieldOf("rotation").forGetter(RotateBlockFunction::rotation)));

  @Override
  public @NotNull String asString() {
    return "rotate(" + rotation.asString() + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    return blockState.rotate(rotation.apply(world.getRandom()));
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.ROTATE;
  }

  public enum Type implements BlockFunctionType<RotateBlockFunction> {
    ROTATE_TYPE;

    @Override
    public @NotNull MapCodec<RotateBlockFunction> getCodec() {
      return CODEC;
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
    public RotateBlockFunction getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) {
      return new RotateBlockFunction(rotation);
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      rotation = EnumOrRandom.parseAndSuggest(BlockRotation.values(), BlockRotation.CODEC, parser);
    }
  }
}
