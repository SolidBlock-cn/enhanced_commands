package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;

public class EnhancedCommandsDataGenerator implements DataGeneratorEntrypoint {
  @Override
  public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
    final FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

    // 仅在数据生成时作为动态注册表注册
    DynamicRegistries.register(BlockFunction.REGISTRY_KEY, BlockFunction.CODEC);
    DynamicRegistries.register(BlockPredicate.REGISTRY_KEY, BlockPredicate.CODEC);

    pack.addProvider(BlockFunctionDataGeneration::new);
    pack.addProvider(BlockPredicateDataGeneration::new);
    pack.addProvider(BlockTagDataGenerator::new);
  }
}
