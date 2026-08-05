package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.enchantment.function.AddEnchantmentsFunction;
import pers.solid.ecmd.enchantment.function.EnchantmentLevelProvider;
import pers.solid.ecmd.enchantment.function.EnchantmentModificationTarget;
import pers.solid.ecmd.enchantment.function.NaturalEnchantmentsFunction;
import pers.solid.ecmd.item.function.EnchantItemFunction;
import pers.solid.ecmd.item.function.ItemFunction;

import java.util.List;
import java.util.Optional;

public class ItemFunctionDataGeneration implements DynamicRegistryGenerationBridge<ItemFunction> {
  private static ResourceKey<ItemFunction> of(String value) {
    return ResourceKey.create(ItemFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Item Functions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<ItemFunction> context) {
    context.add(of("enchant/randomly"), new EnchantItemFunction(List.of(new NaturalEnchantmentsFunction(ConstantValue.exactly(30), Optional.empty()))));
    context.add(of("enchant/super_reasonable"), new EnchantItemFunction(List.of(new AddEnchantmentsFunction(
        EnchantmentModificationTarget.Special.ALL_SUPPORTED,
        EnchantmentLevelProvider.Special.MAX_REASONABLE,
        false,
        false,
        false
    ))));
    context.add(of("enchant/super_extreme"), new EnchantItemFunction(List.of(new AddEnchantmentsFunction(
        EnchantmentModificationTarget.Special.ALL_SUPPORTED,
        EnchantmentLevelProvider.Special.MAX_POSSIBLE,
        false,
        false,
        false
    ))));
    context.add(of("enchant/super_everything"), new EnchantItemFunction(List.of(new AddEnchantmentsFunction(
        EnchantmentModificationTarget.Special.ALL,
        EnchantmentLevelProvider.Special.MAX_POSSIBLE,
        false,
        false,
        false
    ))));
    context.add(of("enchant/mess"), new EnchantItemFunction(List.of(new AddEnchantmentsFunction(
        EnchantmentModificationTarget.Special.ALL,
        EnchantmentLevelProvider.Special.RANDOM_POSSIBLE,
        false,
        false,
        false
    ))));
  }
}
