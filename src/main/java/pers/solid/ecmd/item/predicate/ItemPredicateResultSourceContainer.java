package pers.solid.ecmd.item.predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.ExecutionContext;

@ApiStatus.Internal
public class ItemPredicateResultSourceContainer implements ItemPredicateArgument.Result {
  private final ItemPredicateArgument.Result forward;
  private final ItemPredicate modItemPredicate;
  private @Nullable ExecutionContext cachedContext;

  public ItemPredicateResultSourceContainer(ItemPredicateArgument.Result forward, ItemPredicate modItemPredicate) {
    this.forward = forward;
    this.modItemPredicate = modItemPredicate;
  }

  @Override
  public boolean test(ItemStack itemStack) {
    if (cachedContext != null) {
      return modItemPredicate.test(itemStack, cachedContext);
    }
    EnhancedCommands.LOGGER.warn("Calling 'ItemPredicateResultSourceContainer.test' without calling 'setSource'.");
    return forward.test(itemStack);
  }

  public void setSource(CommandSourceStack source) {
    this.cachedContext = new ExecutionContext(source);
  }
}
