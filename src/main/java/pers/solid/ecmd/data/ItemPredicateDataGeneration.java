package pers.solid.ecmd.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.item.predicate.ComponentPresenceItemPredicate;
import pers.solid.ecmd.item.predicate.ItemPredicate;

public class ItemPredicateDataGeneration implements DynamicRegistryGenerationBridge<ItemPredicate> {
  private static ResourceKey<ItemPredicate> of(String value) {
    return ResourceKey.create(ItemPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Item Predicates (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<ItemPredicate> context) {
    context.add(of("enchanted"), new ComponentPresenceItemPredicate<>(DataComponents.ENCHANTMENTS));
  }
}
