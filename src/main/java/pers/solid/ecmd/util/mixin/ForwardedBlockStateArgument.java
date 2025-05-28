package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionArgument;
import pers.solid.ecmd.function.block.BlockFunctionContext;

import java.util.Set;

public class ForwardedBlockStateArgument extends BlockStateArgument {
  private final BlockFunctionArgument blockFunction;
  private @Nullable BlockFunction sourcedBlockFunction = null;
  private ServerCommandSource source;

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
      throw new CommandRuntimeException(e);
    }
  }

  @Override
  public boolean setBlockState(ServerWorld world, BlockPos pos, int flags) {
    if (sourcedBlockFunction != null) {
      if (source == null) {
        EnhancedCommands.LOGGER.warn("Enhanced Commands: Invoking ForwardedBlockStateArgument.setBlockState without source specified! This may cause potential issues. It is usually called when invoking vanilla BlockStateArgumentType.getBlockState.");
        source = world.getServer().getCommandSource();
      }
      return sourcedBlockFunction.setBlock(world, pos, new BlockFunctionContext(flags, 0, world.random, source, null));
    } else {
      return false;
    }
  }
}
