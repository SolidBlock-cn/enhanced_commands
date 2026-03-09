package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * 用于 Fabric 模组的数据生成。
 */
public class EnhancedCommandsDataGenerationImpl implements DataGeneratorEntrypoint {
  @Override
  public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
    final FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

    pack.addProvider(BlockFunctionDataGenerationImpl::new);
    pack.addProvider(BlockPredicateDataGenerationImpl::new);
    pack.addProvider(BlockTagDataGeneratorImpl::new);
  }
}
