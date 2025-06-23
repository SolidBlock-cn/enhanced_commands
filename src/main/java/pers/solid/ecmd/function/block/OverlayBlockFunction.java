package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 叠加多个方块函数，依次应用。
 */
public record OverlayBlockFunction(List<BlockFunction> functions) implements BlockFunction {
  public static final MapCodec<OverlayBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(OverlayBlockFunction::new, BlockFunction.CODEC.listOf().fieldOf("predicates").forGetter(OverlayBlockFunction::functions)));

  public OverlayBlockFunction(BlockFunction... functions) {
    this(List.of(functions));
  }

  @Override
  public @NotNull String asString() {
    return "overlay(" + functions.stream().map(BlockFunction::asString).collect(Collectors.joining(", ")) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    for (BlockFunction blockFunction : functions) {
      blockState = blockFunction.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    }
    return blockState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.OVERLAY;
  }

  public enum Type implements BlockFunctionType<OverlayBlockFunction> {
    OVERLAY_TYPE;

    @Override
    public @NotNull MapCodec<OverlayBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionParamsParser<OverlayBlockFunction> {
    private final List<BlockFunction> blockFunctions = new ArrayList<>();

    @Override
    public OverlayBlockFunction getParseResult(ParseContext<?> parseContext) {
      final ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunction blockFunction : blockFunctions) {
        builder.add(blockFunction);
      }
      return new OverlayBlockFunction(builder.build());
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      blockFunctions.add(BlockFunction.parse(parseContext));
    }
  }
}
