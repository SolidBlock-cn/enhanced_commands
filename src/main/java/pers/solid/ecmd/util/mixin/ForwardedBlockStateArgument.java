package pers.solid.ecmd.util.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;

import java.util.Set;

public class ForwardedBlockStateArgument extends BlockInput {
  private final BlockFunction blockFunction;
  private @Nullable CommandSourceStack source;

  public ForwardedBlockStateArgument(BlockFunction blockFunction) {
    super(Blocks.AIR.defaultBlockState(), Set.of(), null);
    this.blockFunction = blockFunction;
  }

  @Override
  public boolean test(BlockInWorld blockInWorld) {
    return true;
  }

  @Override
  public boolean test(ServerLevel world, BlockPos pos) {
    return true;
  }

  public void setSource(CommandSourceStack source) {
    this.source = source;
  }

  @Override
  public boolean place(ServerLevel world, BlockPos pos, int flags) {
    if (blockFunction != null) {
      if (source == null) {
        EnhancedCommands.LOGGER.warn("Enhanced Commands: Invoking ForwardedBlockStateArgument.setBlockState without source specified! This may cause potential issues. It is usually called when invoking vanilla BlockStateArgumentType.getBlockState.");
        source = world.getServer().createCommandSourceStack();
      }
      return blockFunction.setBlock(world, pos, new BlockFunctionContext(flags, 0, world.random, source, null));
    } else {
      return false;
    }
  }
}
