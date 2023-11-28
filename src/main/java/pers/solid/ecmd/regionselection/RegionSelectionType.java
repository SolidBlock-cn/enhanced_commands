package pers.solid.ecmd.regionselection;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public interface RegionSelectionType {
  RegistryKey<Registry<RegionSelectionType>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(EnhancedCommands.MOD_ID, "region_builder_type"));
  Registry<RegionSelectionType> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  RegionSelection createRegionSelection();

  default RegionSelection createRegionSelectionFrom(RegionSelection source) {
    final RegionSelection regionSelection = createRegionSelection();
    regionSelection.inheritPointsFrom(source);
    return regionSelection;
  }
}
