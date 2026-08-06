package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import pers.solid.ecmd.data.*;

/**
 * 用于 Fabric 模组的数据生成。
 */
public class EnhancedCommandsDataGenerationImpl implements DataGeneratorEntrypoint {
  @Override
  public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
    final FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

    pack.addProvider((output, registriesFuture) -> new TagGenerationBridgeImpl.ForBlock(output, registriesFuture, new BlockTagDataGeneration()));
    addDynamicRegistryGeneration(pack, new BlockFunctionDataGeneration());
    addDynamicRegistryGeneration(pack, new BlockPredicateDataGeneration());
    addDynamicRegistryGeneration(pack, new EnchantmentsFunctionDataGeneration());
    addDynamicRegistryGeneration(pack, new EntityPredicateDataGeneration());
    addDynamicRegistryGeneration(pack, new ItemFunctionDataGeneration());
    addDynamicRegistryGeneration(pack, new ItemPredicateDataGeneration());
    addDynamicRegistryGeneration(pack, new NbtFunctionDataGeneration());
    addDynamicRegistryGeneration(pack, new NbtPredicateDataGeneration());
    addDynamicRegistryGeneration(pack, new NumberProviderDataGeneration());
    addDynamicRegistryGeneration(pack, new RegionProviderDataGeneration());
  }

  private static <T> void addDynamicRegistryGeneration(FabricDataGenerator.Pack pack, DynamicRegistryGenerationBridge<T> bridge) {
    pack.addProvider((output, registriesFuture) -> new DynamicRegistryGenerationBridgeImpl<>(output, registriesFuture, bridge));
  }
}
