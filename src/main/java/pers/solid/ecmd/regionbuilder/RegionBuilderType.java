package pers.solid.ecmd.regionbuilder;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public interface RegionBuilderType {
  RegionBuilder createRegionBuilder();

  default RegionBuilder createRegionBuilderFrom(RegionBuilder source) {
    final RegionBuilder regionBuilder = createRegionBuilder();
    regionBuilder.inheritPointsFrom(source);
    return regionBuilder;
  }

  RegistryKey<Registry<RegionBuilderType>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(EnhancedCommands.MOD_ID, "region_builder_type"));
  Registry<RegionBuilderType> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
}
