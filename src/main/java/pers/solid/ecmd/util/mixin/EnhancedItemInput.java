package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.item.function.ItemComponentCombinationItemFunction;
import pers.solid.ecmd.item.function.ItemFunction;
import pers.solid.ecmd.item.function.SimpleItemFunction;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;

public class EnhancedItemInput extends ItemInput {
  private final ItemFunction itemFunction;
  private @Nullable CommandSourceStack source;

  public EnhancedItemInput(ItemFunction itemFunction) {
    super(getItem(itemFunction), DataComponentPatch.EMPTY);
    this.itemFunction = itemFunction;
  }

  @SuppressWarnings("deprecation")
  private static Holder<Item> getItem(ItemFunction itemFunction) {
    return switch (itemFunction) {
      case SimpleItemFunction(Holder<Item> item) -> item;
      case ItemComponentCombinationItemFunction(ItemFunction base, List<ItemFunction> affiliate) when base instanceof SimpleItemFunction(Holder<Item> item) -> item;
      default -> Items.AIR.builtInRegistryHolder();
    };
  }

  public void setSource(CommandSourceStack source) {
    this.source = source;
  }

  @Override
  public ItemStack createItemStack(int count, boolean allowOversizedStacks) throws CommandSyntaxException {
    if (source == null) {
      EnhancedCommands.LOGGER.warn("Enhanced Commands: Invoking EnhancedItemInput.createItemStack without source specified! This may cause potential issues.");
      super.createItemStack(count, allowOversizedStacks);
    }
    final ItemStack stack = itemFunction.getModifiedStack(ItemStack.EMPTY, ItemStack.EMPTY, new ExecutionContext(source));
    stack.setCount(count);
    return stack;
  }
}
