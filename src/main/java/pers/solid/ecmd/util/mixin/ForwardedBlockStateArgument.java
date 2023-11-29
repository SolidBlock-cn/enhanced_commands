package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionArgument;

import java.util.Set;

public class ForwardedBlockStateArgument extends BlockStateArgument {
  private final BlockFunctionArgument blockFunction;
  private @Nullable BlockFunction sourcedBlockFunction = null;

  public ForwardedBlockStateArgument(BlockFunctionArgument blockFunction) {
    super(Blocks.AIR.getDefaultState(), Set.of(), null);
    this.blockFunction = blockFunction;
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return true;
  }

  @Override
  public boolean test(ServerWorld world, BlockPos pos) {
    return true;
  }

  public void setSource(@NotNull ServerCommandSource source) {
    try {
      this.sourcedBlockFunction = blockFunction.apply(source);
    } catch (CommandSyntaxException e) {
      throw new CommandException(Texts.toText(e.getRawMessage()));
    }
  }

  @Override
  public boolean setBlockState(ServerWorld world, BlockPos pos, int flags) {
    if (sourcedBlockFunction != null) {
      return sourcedBlockFunction.setBlock(world, pos, flags, FillReplaceCommand.POST_PROCESS_FLAG);
    } else {
      return false;
    }
  }
}
