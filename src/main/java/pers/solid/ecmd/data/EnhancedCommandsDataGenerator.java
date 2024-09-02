package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class EnhancedCommandsDataGenerator implements DataGeneratorEntrypoint {
  @Override
  public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
    final FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
    pack.addProvider(BlockFunctionDataGeneration::new);
    pack.addProvider(BlockPredicateDataGeneration::new);
    pack.addProvider(BlockTagDataGenerator::new);
  }
}
