package pers.solid.ecmd.function.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 叠加多个方块函数，依次应用。
 */
public record OverlayBlockFunction(Collection<BlockFunction> blockFunctions) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "overlay(" + blockFunctions.stream().map(BlockFunction::asString).collect(Collectors.joining(", ")) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    for (BlockFunction blockFunction : blockFunctions) {
      blockState = blockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
    return blockState;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    final NbtList nbtList = new NbtList();
    nbtCompound.put("functions", nbtList);
    nbtList.addAll(Collections2.transform(blockFunctions, BlockFunction::createNbt));
  }

  @Override
  public @NotNull BlockFunctionType<OverlayBlockFunction> getType() {
    return BlockFunctionTypes.OVERLAY;
  }

  public enum Type implements BlockFunctionType<OverlayBlockFunction> {
    OVERLAY_TYPE;

    @Override
    public @NotNull OverlayBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new OverlayBlockFunction(ImmutableList.copyOf(Lists.transform(nbtCompound.getList("functions", NbtElement.COMPOUND_TYPE), nbtElement -> BlockFunction.fromNbt((NbtCompound) nbtElement, world))));
    }

    @Override
    public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  private static final class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    private final List<BlockFunctionArgument> blockFunctions = new ArrayList<>();

    @Override
    public @NotNull String functionName() {
      return "overlay";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.block_function.overlay");
    }

    @Override
    public BlockFunctionArgument getParseResult(SuggestedParser parser) {
      return source -> {
        final ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
        for (BlockFunctionArgument blockFunction : blockFunctions) {
          builder.add(blockFunction.apply(source));
        }
        return new OverlayBlockFunction(builder.build());
      };
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      blockFunctions.add(BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly));
    }
  }
}
