package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.WorldAccess;
import pers.solid.ecmd.predicate.block.*;

public class ForwardingBlockPredicateArgument implements BlockPredicateArgumentType.BlockPredicate {
  private final BlockPredicateArgument modBlockPredicate;
  private BlockPredicate sourcedBlockPredicate = null;

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
      return sourcedBlockPredicate.test(cachedBlockPosition, new BlockPredicateContext(((WorldAccess) cachedBlockPosition.getWorld()).getRandom(), null));
    }
    return false;
  }

  public void setSource(ServerCommandSource source) throws CommandSyntaxException {
    this.sourcedBlockPredicate = modBlockPredicate.apply(source);
  }
}
