package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.number.EnhancedCommandsNumberProvider;

public class NumberProviderDataGeneration implements DynamicRegistryGenerationBridge<NumberProvider> {
  private static ResourceKey<NumberProvider> of(String value) {
    return ResourceKey.create(EnhancedCommandsNumberProvider.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Number Providers (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<NumberProvider> context) {
    context.add(of("examples/uniform_0_100"), UniformGenerator.between(0, 100));
    context.add(of("examples/binomial_standard"), BinomialDistributionGenerator.binomial(0, 1));
  }
}
