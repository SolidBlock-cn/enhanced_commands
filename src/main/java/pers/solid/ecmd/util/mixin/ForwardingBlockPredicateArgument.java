package pers.solid.ecmd.util.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.block.predicate.NbtBlockPredicate;
import pers.solid.ecmd.block.predicate.PropertiesNbtCombinationBlockPredicate;
import pers.solid.ecmd.util.ExecutionContext;

public class ForwardingBlockPredicateArgument implements BlockPredicateArgument.Result {
  private final BlockPredicate modBlockPredicate;
  private @Nullable CommandSourceStack source;

  public ForwardingBlockPredicateArgument(BlockPredicate modBlockPredicate) {
    this.modBlockPredicate = modBlockPredicate;
  }

  @Override
  public boolean requiresNbt() {
    return modBlockPredicate instanceof NbtBlockPredicate || modBlockPredicate instanceof PropertiesNbtCombinationBlockPredicate p && p.nbt() != null;
  }

  @Override
  public boolean test(BlockInWorld blockInWorld) {
    if (modBlockPredicate != null) {
      if (source == null) {
        EnhancedCommands.LOGGER.warn("Enhanced Commands: Invoking ForwardedBlockPredicateArgument.test without source specified! This may cause potential issues. It is usually called when invoking vanilla BlockPredicateArgumentType.getBlockPredicate.");
        source = ((ServerLevel) blockInWorld.getLevel()).getServer().createCommandSourceStack();
      }
      return modBlockPredicate.test(blockInWorld, new ExecutionContext(source, null));
    }
    return false;
  }

  public void setSource(CommandSourceStack source) {
    this.source = source;
  }
}
