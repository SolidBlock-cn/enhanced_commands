package pers.solid.ecmd.util.mixin;

import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.predicate.block.NbtBlockPredicate;
import pers.solid.ecmd.predicate.block.PropertiesNbtCombinationBlockPredicate;

public class ForwardingBlockPredicateArgument implements BlockPredicateArgumentType.BlockPredicate {
  private final BlockPredicateArgument modBlockPredicate;
  private BlockPredicate sourcedBlockPredicate = null;

  public ForwardingBlockPredicateArgument(BlockPredicateArgument modBlockPredicate) {this.modBlockPredicate = modBlockPredicate;}

  @Override
  public boolean hasNbt() {
    return modBlockPredicate instanceof NbtBlockPredicate || modBlockPredicate instanceof PropertiesNbtCombinationBlockPredicate p && p.nbtBlockPredicate() != null;
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (sourcedBlockPredicate != null) {
      return sourcedBlockPredicate.test(cachedBlockPosition);
    }
    return false;
  }

  public void setSource(ServerCommandSource source) {
    this.sourcedBlockPredicate = modBlockPredicate.apply(source);
  }
}
