package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import pers.solid.ecmd.data.ItemPredicateDataGeneration;
import pers.solid.ecmd.item.predicate.ItemPredicate;

import java.util.concurrent.CompletableFuture;

public class ItemPredicateDataGenerationImpl extends FabricDynamicRegistryProvider implements ItemPredicateDataGeneration, DynamicRegistryGenerationBridgeImpl<ItemPredicate> {
  public ItemPredicateDataGenerationImpl(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(HolderLookup.Provider registries, Entries entries) {
    configureBridge(registries, entries);
  }

  @Override
  public String getName() {
    return "Item Predicates (Enhanced Commands)";
  }
}
