package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import pers.solid.ecmd.data.ItemFunctionDataGeneration;
import pers.solid.ecmd.item.function.ItemFunction;

import java.util.concurrent.CompletableFuture;

public class ItemFunctionDataGenerationImpl extends FabricDynamicRegistryProvider implements ItemFunctionDataGeneration, DynamicRegistryGenerationBridgeImpl<ItemFunction> {
  public ItemFunctionDataGenerationImpl(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(HolderLookup.Provider registries, Entries entries) {
    configureBridge(registries, entries);
  }

  @Override
  public String getName() {
    return "Item Functions (Enhanced Commands)";
  }
}
