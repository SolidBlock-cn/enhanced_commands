package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.enchantment.function.AddEnchantmentModification;
import pers.solid.ecmd.enchantment.function.EnchantmentLevelProvider;
import pers.solid.ecmd.enchantment.function.EnchantmentModificationTarget;
import pers.solid.ecmd.enchantment.function.NaturalEnchantmentModification;
import pers.solid.ecmd.item.function.EnchantItemFunction;
import pers.solid.ecmd.item.function.ItemFunction;

import java.util.List;
import java.util.Optional;

public interface ItemFunctionDataGeneration extends DynamicRegistryGenerationBridge<ItemFunction> {
  static ResourceKey<ItemFunction> of(String value) {
    return ResourceKey.create(ItemFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  default void configureBridge(ContextBridge<ItemFunction> context) {
    context.add(of("enchant_randomly"), new EnchantItemFunction(List.of(new NaturalEnchantmentModification(ConstantValue.exactly(30), Optional.empty()))));
    context.add(of("super_reasonable_enchant"), new EnchantItemFunction(List.of(new AddEnchantmentModification(
        EnchantmentModificationTarget.Special.ALL_SUPPORTED,
        EnchantmentLevelProvider.Special.MAX_REASONABLE,
        false,
        false,
        false
    ))));
    context.add(of("super_extreme_enchant"), new EnchantItemFunction(List.of(new AddEnchantmentModification(
        EnchantmentModificationTarget.Special.ALL_SUPPORTED,
        EnchantmentLevelProvider.Special.MAX_POSSIBLE,
        false,
        false,
        false
    ))));
    context.add(of("super_everything_enchant"), new EnchantItemFunction(List.of(new AddEnchantmentModification(
        EnchantmentModificationTarget.Special.ALL,
        EnchantmentLevelProvider.Special.MAX_POSSIBLE,
        false,
        false,
        false
    ))));
    context.add(of("mess_enchant"), new EnchantItemFunction(List.of(new AddEnchantmentModification(
        EnchantmentModificationTarget.Special.ALL,
        EnchantmentLevelProvider.Special.RANDOM_POSSIBLE,
        false,
        false,
        false
    ))));
  }
}
