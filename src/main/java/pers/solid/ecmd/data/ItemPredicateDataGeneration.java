package pers.solid.ecmd.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.item.predicate.AnyItemPredicate;
import pers.solid.ecmd.item.predicate.ComponentPresenceItemPredicate;
import pers.solid.ecmd.item.predicate.ItemPredicate;
import pers.solid.ecmd.item.predicate.SimpleItemPredicate;

import java.util.List;

public class ItemPredicateDataGeneration implements DynamicRegistryGenerationBridge<ItemPredicate> {
  static ResourceKey<ItemPredicate> of(String value) {
    return ResourceKey.create(ItemPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Item Predicates (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<ItemPredicate> context) {
    context.add(of("diamond_or_emerald"), new AnyItemPredicate(List.of(new SimpleItemPredicate(Items.DIAMOND), new SimpleItemPredicate(Items.EMERALD))));
    context.add(of("diamonds"), new AnyItemPredicate(List.of(new SimpleItemPredicate(Items.DIAMOND), new SimpleItemPredicate(Items.DIAMOND_BLOCK))));
//    context.add(of("fire_resistant"), new ComponentPresenceItemPredicate<>(DataComponents.FIRE_RESISTANT)); todo 转化为符合新版本的谓词
    context.add(of("enchanted"), new ComponentPresenceItemPredicate<>(DataComponents.ENCHANTMENTS));
  }
}
