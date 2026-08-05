package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.enchantment.function.EnchantmentsFunction;
import pers.solid.ecmd.enchantment.function.NaturalEnchantmentsFunction;

public class EnchantmentsFunctionDataGeneration implements DynamicRegistryGenerationBridge<EnchantmentsFunction> {
  private static ResourceKey<EnchantmentsFunction> of(String value) {
    return ResourceKey.create(EnchantmentsFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Enchantment Functions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<EnchantmentsFunction> context) {
    context.add(of("natural/30"), new NaturalEnchantmentsFunction(ConstantValue.exactly(30f)));
    context.add(of("natural/random"), new NaturalEnchantmentsFunction(UniformGenerator.between(0f, 30f)));
  }
}
