package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.predicate.block.*;

public class ForwardingBlockPredicateArgument implements BlockPredicateArgumentType.BlockPredicate {
  private final BlockPredicateArgument modBlockPredicate;
  private BlockPredicate sourcedBlockPredicate = null;
  private ServerCommandSource source;

  public ForwardingBlockPredicateArgument(BlockPredicateArgument modBlockPredicate) {
    this.modBlockPredicate = modBlockPredicate;
  }

  @Override
  public boolean hasNbt() {
    return modBlockPredicate instanceof NbtBlockPredicate || modBlockPredicate instanceof PropertiesNbtCombinationBlockPredicate p && p.nbt() != null;
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (sourcedBlockPredicate != null) {
      if (source == null) {
        EnhancedCommands.LOGGER.warn("Enhanced Commands: Invoking ForwardedBlockPredicateArgument.test without source specified! This may cause potential issues. It is usually called when invoking vanilla BlockPredicateArgumentType.getBlockPredicate.");
        source = ((ServerWorld) cachedBlockPosition.getWorld()).getServer().getCommandSource();
      }
      return sourcedBlockPredicate.test(cachedBlockPosition, new ExecutionContext(source, null));
    }
    return false;
  }

  public void setSource(ServerCommandSource source) throws CommandSyntaxException {
    this.sourcedBlockPredicate = modBlockPredicate.apply(source);
  }
}
